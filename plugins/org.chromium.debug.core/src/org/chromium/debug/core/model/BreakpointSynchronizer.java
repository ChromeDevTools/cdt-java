// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.debug.core.util.ChromiumDebugPluginUtil.getSafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.ChromiumSourceDirector;
import org.chromium.debug.core.ScriptNameManipulator.ScriptNamePattern;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * A class responsible for comparing breakpoints in workspace and on remote VM and synchronizing
 * them in both directions. {@link Direction#RESET_REMOTE} allows several synchronization
 * jobs to different VMs.
 */
public class BreakpointSynchronizer {
  private final JavascriptVm javascriptVm;
  private final BreakpointMap.InTargetMap breakpointInTargetMap;
  private final ChromiumSourceDirector sourceDirector;
  private final BreakpointHelper breakpointHelper;
  private final String debugModelId;

  public BreakpointSynchronizer(JavascriptVm javascriptVm,
      BreakpointMap.InTargetMap breakpointInTargetMap,
      ChromiumSourceDirector sourceDirector, BreakpointHelper breakpointHelper,
      String debugModelId) {
    this.javascriptVm = javascriptVm;
    this.breakpointInTargetMap = breakpointInTargetMap;
    this.sourceDirector = sourceDirector;
    this.breakpointHelper = breakpointHelper;
    this.debugModelId = debugModelId;
  }

  /**
   * Describes a direction the breakpoint synchronization should be performed in.
   */
  public enum Direction {

    /**
     * All breakpoints in remote VM/VMs are cleared/updated/created to conform to breakpoints in
     * Eclipse workspace.
     */
    RESET_REMOTE,

    /**
     * All breakpoints in local workspace are cleared/updated/created to conform to breakpoints in
     * remote VM (not applicable for multiple VMs).
     */
    RESET_LOCAL,

    /**
     * Breakpoints are created locally or remotely or tied together so that every breakpoint
     * has a counterpart on other side.
     */
    MERGE
  }

  /**
   * Additional interface used by {@link BreakpointSynchronizer}.
   */
  public interface BreakpointHelper {
    /**
     * Create breakpoint on remote VM (asynchronously) and link it to uiBreakpoint.
     */
    RelayOk createBreakpointOnRemote(WrappedBreakpoint uiBreakpoint, VmResourceRef vmResourceRef,
        CreateCallback createCallback, SyncCallback syncCallback) throws CoreException;

    interface CreateCallback {
      void failure(Exception ex);
      void success();
    }
  }

  public interface Callback {
    void onDone(IStatus status);
  }

  /**
   * The main entry method of the class. Asynchronously performs synchronization job.
   */
  public void syncBreakpoints(Direction direction, Callback callback) {
    ReportBuilder reportBuilder = new ReportBuilder(direction);
    StatusBuilder statusBuilder = new StatusBuilder(callback, reportBuilder);

    statusBuilder.plan(UNCODITIONALLY_RELAY_TO_REST_OF_METHOD_OK);
    Exception ex = null;
    try {
      syncBreakpointsImpl(direction, statusBuilder);
    } catch (RuntimeException e) {
      ex = e;
    } finally {
      statusBuilder.done(ex);
    }
  }

  private static final RelayOk UNCODITIONALLY_RELAY_TO_REST_OF_METHOD_OK = new RelayOk() {};

  private void syncBreakpointsImpl(final Direction direction, final StatusBuilder statusBuilder) {
    // Collect the remote breakpoints.
    Collection<? extends Breakpoint> sdkBreakpoints = readSdkBreakpoints(javascriptVm);
    // Collect all local breakpoints.
    Set<WrappedBreakpoint> uiBreakpoints = getUiBreakpoints();

    List<Breakpoint> sdkBreakpoints2 = new ArrayList<Breakpoint>(sdkBreakpoints.size());

    if (direction != Direction.MERGE) {
      breakpointInTargetMap.clear();
    }

    // Throw away all already linked breakpoints and put remaining into sdkBreakpoints2 list.
    for (Breakpoint sdkBreakpoint : sdkBreakpoints) {
      WrappedBreakpoint uiBreakpoint = breakpointInTargetMap.getUiBreakpoint(sdkBreakpoint);
      if (uiBreakpoint == null) {
        // No mapping. Schedule for further processing.
        sdkBreakpoints2.add(sdkBreakpoint);
      } else {
        // There is a live mapping. This set should also contain this breakpoint.
        uiBreakpoints.remove(uiBreakpoint);
        statusBuilder.getReportBuilder().increment(ReportBuilder.Property.LINKED);
      }
    }

    // Sort all breakpoints by (script_name, line_number).
    SortedBreakpoints<WrappedBreakpoint> sortedUiBreakpoints =
        sortBreakpoints(uiBreakpoints, uiBreakpointHandler);
    SortedBreakpoints<Breakpoint> sortedSdkBreakpoints =
        sortBreakpoints(sdkBreakpoints2, sdkBreakpointHandler);

    BreakpointMerger breakpointMerger = new BreakpointMerger(direction, breakpointInTargetMap);

    // Find all unlinked breakpoints on both sides.
    mergeBreakpoints(breakpointMerger, sortedUiBreakpoints, sortedSdkBreakpoints);

    List<Breakpoint> sdkBreakpointsToDelete;
    List<Breakpoint> sdkBreakpointsToCreate;
    List<WrappedBreakpoint> uiBreakpointsToDelete;
    List<WrappedBreakpoint> uiBreakpointsToCreate;

    // Plan actions for all breakpoints without pair.
    if (direction == Direction.RESET_REMOTE) {
      sdkBreakpointsToDelete = breakpointMerger.getMissingSdk();
      sdkBreakpointsToCreate = Collections.emptyList();
    } else {
      sdkBreakpointsToCreate = breakpointMerger.getMissingSdk();
      sdkBreakpointsToDelete = Collections.emptyList();
    }

    if (direction == Direction.RESET_LOCAL) {
      uiBreakpointsToDelete = breakpointMerger.getMissingUi();
      uiBreakpointsToCreate = Collections.emptyList();
    } else {
      uiBreakpointsToCreate = breakpointMerger.getMissingUi();
      uiBreakpointsToDelete = Collections.emptyList();
    }

    // First delete everything, then create (we may need to re-create some breakpoints, so order
    // is significant).
    deteleBreakpoints(sdkBreakpointsToDelete, uiBreakpointsToDelete, statusBuilder);
    createBreakpoints(sdkBreakpointsToCreate, uiBreakpointsToCreate, statusBuilder);
  }

  private void deteleBreakpoints(List<Breakpoint> sdkBreakpointsToDelete,
      List<WrappedBreakpoint> uiBreakpointsToDelete, final StatusBuilder statusBuilder) {
    for (Breakpoint sdkBreakpoint : sdkBreakpointsToDelete) {
      final PlannedTaskHelper deleteTaskHelper = new PlannedTaskHelper(statusBuilder);
      JavascriptVm.BreakpointCallback callback = new JavascriptVm.BreakpointCallback() {
        public void failure(String errorMessage) {
          deleteTaskHelper.setException(new Exception(errorMessage));
        }
        public void success(Breakpoint breakpoint) {
          statusBuilder.getReportBuilder().increment(ReportBuilder.Property.DELETED_ON_REMOTE);
        }
      };
      RelayOk relayOk = sdkBreakpoint.clear(callback, deleteTaskHelper);
      deleteTaskHelper.registerSelf(relayOk);
    }
    for (WrappedBreakpoint uiBreakpoint : uiBreakpointsToDelete) {
      ChromiumLineBreakpoint.getIgnoreList().add(uiBreakpoint);
      try {
        try {
          uiBreakpoint.getInner().delete();
        } catch (CoreException e) {
          throw new RuntimeException(e);
        }
      } finally {
        ChromiumLineBreakpoint.getIgnoreList().remove(uiBreakpoint);
      }
      statusBuilder.getReportBuilder().increment(ReportBuilder.Property.DELETED_LOCALLY);
    }
  }

  private void createBreakpoints(List<Breakpoint> sdkBreakpointsToCreate,
      List<WrappedBreakpoint> uiBreakpointsToCreate, final StatusBuilder statusBuilder) {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    for (Breakpoint sdkBreakpoint : sdkBreakpointsToCreate) {
      Object sourceElement = sourceDirector.getSourceElement(sdkBreakpoint);
      if (sourceElement instanceof IFile == false) {
        statusBuilder.getReportBuilder().addProblem(
            ReportBuilder.Problem.UNRESOLVED_REMOTE_BREAKPOINT,
            sdkBreakpoint.getTarget().accept(BREAKPOINT_DEBUG_DESTINATION_VISITOR));
        continue;
      }
      // We do not actually support working files for scripts with offset.
      int script_line_offset = 0;
      IFile resource = (IFile) sourceElement;
      WrappedBreakpoint uiBreakpoint;
      try {
        ChromiumLineBreakpoint uiBreakpointInner = ChromiumLineBreakpoint.Helper.createLocal(
            sdkBreakpoint, breakpointManager, resource, script_line_offset, debugModelId);
        uiBreakpoint = ChromiumBreakpointAdapter.wrap(uiBreakpointInner);
        breakpointInTargetMap.add(sdkBreakpoint, uiBreakpoint);
      } catch (CoreException e) {
        throw new RuntimeException(e);
      }
      statusBuilder.getReportBuilder().increment(ReportBuilder.Property.CREATED_LOCALLY);
    }
    for (WrappedBreakpoint uiBreakpoint : uiBreakpointsToCreate) {
      VmResourceRef vmResourceRef = uiBreakpointHandler.getVmResourceRef(uiBreakpoint);
      if (vmResourceRef == null) {
        // Actually we should not get here, because getScript call succeeded before.
        continue;
      }

      final PlannedTaskHelper createTaskHelper = new PlannedTaskHelper(statusBuilder);
      BreakpointHelper.CreateCallback createCallback = new BreakpointHelper.CreateCallback() {
        public void success() {
          statusBuilder.getReportBuilder().increment(ReportBuilder.Property.CREATED_ON_REMOTE);
        }
        public void failure(Exception ex) {
          createTaskHelper.setException(ex);
        }
      };
      try {
        RelayOk relayOk = breakpointHelper.createBreakpointOnRemote(uiBreakpoint, vmResourceRef,
            createCallback, createTaskHelper);
        createTaskHelper.registerSelf(relayOk);
      } catch (CoreException e) {
        statusBuilder.addOnStartException(e);
      }
    }
  }

  private static final Breakpoint.Target.Visitor<String> BREAKPOINT_DEBUG_DESTINATION_VISITOR =
      new BreakpointTypeExtension.ScriptRegExpSupport.Visitor<String>() {
        @Override public String visitScriptName(String scriptName) {
          return "script_name=" + scriptName;
        }
        @Override public String visitScriptId(long scriptId) {
          return "script_id" + scriptId;
        }
        @Override public String visitRegExp(String regExp) {
          return "RegExp: " + regExp;
        }
        @Override public String visitUnknown(Breakpoint.Target target) {
          return "Unknown target: + " + target;
        }
      };

  private static class BreakpointMerger extends Merger<WrappedBreakpoint, Breakpoint> {
    private final Direction direction;
    private final List<WrappedBreakpoint> missingUi = new ArrayList<WrappedBreakpoint>();
    private final List<Breakpoint> missingSdk = new ArrayList<Breakpoint>();
    private final BreakpointMap.InTargetMap breakpointMap;

    BreakpointMerger(Direction direction, BreakpointMap.InTargetMap breakpointMap) {
      this.direction = direction;
      this.breakpointMap = breakpointMap;
    }
    @Override
    void both(WrappedBreakpoint v1, Breakpoint v2) {
      if (direction == Direction.MERGE) {
        breakpointMap.add(v2, v1);
      } else {
        onlyFirst(v1);
        onlySecond(v2);
      }
    }
    @Override
    void onlyFirst(WrappedBreakpoint v1) {
      missingUi.add(v1);
    }
    @Override
    void onlySecond(Breakpoint v2) {
      missingSdk.add(v2);
    }
    List<WrappedBreakpoint> getMissingUi() {
      return missingUi;
    }
    List<Breakpoint> getMissingSdk() {
      return missingSdk;
    }
  }

  /**
   * A class responsible for creating a summary status of synchronization operation. The status
   * is created once all asynchronous jobs have finished. Each job first registers itself
   * via {@link #plan()} method and
   * later reports its result via {@link #done(Exception)} method.
   * When the last job is reporting its finishing, the status gets built and sent to
   * {@link #callback}. If no exceptions were registered,
   * status contains text report from {@link ReportBuilder}.
   */
  private static class StatusBuilder {
    private final Callback callback;
    private int plannedNumber = 0;
    private final List<Exception> exceptions = new ArrayList<Exception>(0);
    private boolean alreadyReported = false;
    private final ReportBuilder reportBuilder;

    StatusBuilder(Callback callback, ReportBuilder reportBuilder) {
      this.callback = callback;
      this.reportBuilder = reportBuilder;
    }

    ReportBuilder getReportBuilder() {
      return reportBuilder;
    }

    public synchronized void plan(RelayOk relayOk) {
      if (alreadyReported) {
        throw new IllegalStateException();
      }
      plannedNumber++;
    }

    public void done(Exception ex) {
      boolean timeToReport = doneImpl(ex);
      if (timeToReport) {
        reportResult();
      }
    }

    public synchronized void addOnStartException(Exception ex) {
      exceptions.add(ex);
    }

    private synchronized boolean doneImpl(Exception ex) {
      if (ex != null) {
        exceptions.add(ex);
      }
      plannedNumber--;
      if (plannedNumber == 0) {
        if (!alreadyReported) {
          alreadyReported = true;
          return true;
        }
      }
      return false;
    }

    private void reportResult() {
      IStatus status;
      if (exceptions.isEmpty()) {
        status = new Status(IStatus.OK, ChromiumDebugPlugin.PLUGIN_ID,
            "Breakpoint synchronization done: " + reportBuilder.build(), null); //$NON-NLS-1$
      } else {
        IStatus[] subStatuses = new IStatus[exceptions.size()];
        for (int i = 0 ; i < subStatuses.length; i++) {
          subStatuses[i] = new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
              exceptions.get(i).getMessage(), exceptions.get(i));
        }
        status = new MultiStatus(ChromiumDebugPlugin.PLUGIN_ID, IStatus.ERROR, subStatuses,
            "Breakpoint synchronization errors", null); //$NON-NLS-1$
      }
      if (callback != null) {
        callback.onDone(status);
      }
    }
  }

  private static class PlannedTaskHelper implements SyncCallback {
    private final StatusBuilder statusBuilder;
    private volatile Exception exception = null;
    PlannedTaskHelper(StatusBuilder statusBuilder) {
      this.statusBuilder = statusBuilder;
    }
    void registerSelf(RelayOk relayOk) {
      statusBuilder.plan(relayOk);
    }
    public void callbackDone(RuntimeException e) {
      if (e != null) {
        exception = e;
      }
      statusBuilder.done(exception);
    }
    void setException(Exception ex) {
      exception = ex;
    }
  }

  /**
   * A class that contains several conunters.
   */
  private static class ReportBuilder {
    enum Property {
      LINKED,
      CREATED_LOCALLY,
      DELETED_LOCALLY,
      CREATED_ON_REMOTE,
      DELETED_ON_REMOTE;
      String getVisibleName() {
        return toString();
      }
    }

    enum Problem {
      UNRESOLVED_REMOTE_BREAKPOINT;
      String getVisibleName() {
        return toString();
      }
    }

    private final Direction direction;
    private final Map<Property, AtomicInteger> counters;
    private final Map<Problem, List<String>> problems;

    ReportBuilder(Direction direction) {
      this.direction = direction;
      counters = new EnumMap<Property, AtomicInteger>(Property.class);
      for (Property property : Property.class.getEnumConstants()) {
        counters.put(property, new AtomicInteger(0));
      }
      problems = new HashMap<Problem, List<String>>(1);
    }

    public void increment(Property property) {
      counters.get(property).addAndGet(1);
    }

    public synchronized void addProblem(Problem problem, String message) {
      List<String> list = getSafe(problems, problem);
      if (list == null) {
        list = new ArrayList<String>();
        problems.put(problem, list);
      }
      list.add(message);
    }

    public String build() {
      StringBuilder builder = new StringBuilder();
      builder.append("direction=").append(direction); //$NON-NLS-1$
      for (Map.Entry<Property, AtomicInteger> en : counters.entrySet()) {
        int number = en.getValue().get();
        if (number == 0) {
          continue;
        }
        builder.append(" ").append(en.getKey().getVisibleName()); //$NON-NLS-1$
        builder.append("=").append(number); //$NON-NLS-1$
      }
      if (!problems.isEmpty()) {
        builder.append('\n').append(problems.toString());
      }
      return builder.toString();
    }
  }

  /**
   * A handler for properties of breakpoint type B that helps reading them.
   */
  private static abstract class PropertyHandler<B> {
    /** @return vm resource name or null */
    abstract VmResourceRef getVmResourceRef(B breakpoint);
    /** @return 0-based number */
    abstract long getLineNumber(B breakpoint);
  }

  private final PropertyHandler<WrappedBreakpoint> uiBreakpointHandler =
      new PropertyHandler<WrappedBreakpoint>() {
      @Override
    long getLineNumber(WrappedBreakpoint chromiumLineBreakpoint) {
      int lineNumber;
      try {
        // TODO(peter.rybin): Consider supporting inline scripts here.
        return chromiumLineBreakpoint.getInner().getLineNumber() - 1;
      } catch (CoreException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    VmResourceRef getVmResourceRef(WrappedBreakpoint chromiumLineBreakpoint) {
      IMarker marker = chromiumLineBreakpoint.getInner().getMarker();
      if (marker == null) {
        return null;
      }
      IResource resource = marker.getResource();
      if (resource instanceof IFile == false) {
        return null;
      }
      IFile file = (IFile) resource;
      try {
        return sourceDirector.findVmResourceRef(file);
      } catch (CoreException e) {
        throw new RuntimeException("Failed to read script name from breakpoint", e); //$NON-NLS-1$
      }
    }
  };

  private static final PropertyHandler<Breakpoint> sdkBreakpointHandler =
      new PropertyHandler<Breakpoint>() {
    @Override
    long getLineNumber(Breakpoint breakpoint) {
      return breakpoint.getLineNumber();
    }

    @Override
    VmResourceRef getVmResourceRef(Breakpoint breakpoint) {
      return breakpoint.getTarget().accept(resourceRefVisitor);
    }

    private final Breakpoint.Target.Visitor<VmResourceRef> resourceRefVisitor =
        new BreakpointTypeExtension.ScriptRegExpSupport.Visitor<VmResourceRef>() {
          @Override
          public VmResourceRef visitScriptName(String scriptName) {
            return VmResourceRef.forVmResourceId(new VmResourceId(scriptName, null));
          }

          @Override
          public VmResourceRef visitScriptId(long scriptId) {
            return VmResourceRef.forVmResourceId(new VmResourceId(null, scriptId));
          }

          @Override
          public VmResourceRef visitRegExp(String regExp) {
            ScriptNamePattern pattern = new ScriptNamePattern(regExp);
            return VmResourceRef.forInaccurate(pattern);
          }

          @Override
          public VmResourceRef visitUnknown(Breakpoint.Target target) {
            return null;
          }
        };
  };

  /**
   * A helping structure that holds field of complicated type.
   */
  private static class SortedBreakpoints<B> {
    final Map<VmResourceRef, Map<Long, B>> data;

    SortedBreakpoints(Map<VmResourceRef, Map<Long, B>> data) {
      this.data = data;
    }
  }

  /**
   * Put all breakpoints into map script_name -> line_number -> breakpoint.
   */
  private static <B> SortedBreakpoints<B> sortBreakpoints(Collection<? extends B> breakpoints,
      PropertyHandler<B> handler) {
    Map<VmResourceRef, Map<Long, B>> result = new HashMap<VmResourceRef, Map<Long, B>>();
    for (B breakpoint : breakpoints) {
      VmResourceRef vmResourceRef = handler.getVmResourceRef(breakpoint);
      if (vmResourceRef == null) {
        continue;
      }
      Map<Long, B> subMap = result.get(vmResourceRef);
      if (subMap == null) {
        subMap = new HashMap<Long, B>(3);
        result.put(vmResourceRef, subMap);
      }
      long line = handler.getLineNumber(breakpoint);
      // For simplicity we ignore multiple breakpoints on the same line.
      subMap.put(line, breakpoint);
    }
    return new SortedBreakpoints<B>(result);
  }

  /**
   * A class that implements merge operation for a particular complete/incomplete pair of values.
   */
  private static abstract class Merger<V1, V2> {
    abstract void onlyFirst(V1 v1);
    abstract void onlySecond(V2 v2);
    abstract void both(V1 v1, V2 v2);
  }

  /**
   * Merges values of 2 maps.
   * @param map2 must implement {@link Map#remove} method.
   */
  private static <K, V1, V2> void mergeMaps(Map<K, V1> map1, Map<K, V2> map2,
      Merger<V1, V2> merger) {
    for (Map.Entry<K, V1> en : map1.entrySet()) {
      V2 v2 = map2.remove(en.getKey());
      if (v2 == null) {
        merger.onlyFirst(en.getValue());
      } else {
        merger.both(en.getValue(), v2);
      }
    }
    for (V2 v2 : map2.values()) {
      merger.onlySecond(v2);
    }
  }

  private static <B1, B2> void mergeBreakpoints(final Merger<B1, B2> perBreakpointMerger,
      SortedBreakpoints<B1> side1, SortedBreakpoints<B2> side2) {
    Merger<Map<Long, B1>, Map<Long, B2>> perScriptMerger =
        new Merger<Map<Long,B1>, Map<Long,B2>>() {
      @Override
      void both(Map<Long, B1> v1, Map<Long, B2> v2) {
        mergeMaps(v1, v2, perBreakpointMerger);
      }

      @Override
      void onlyFirst(Map<Long, B1> v1) {
        mergeMaps(v1, Collections.<Long, B2>emptyMap(), perBreakpointMerger);
      }

      @Override
      void onlySecond(Map<Long, B2> v2) {
        mergeMaps(Collections.<Long, B1>emptyMap(), v2, perBreakpointMerger);
      }
    };
    mergeMaps(side1.data, side2.data, perScriptMerger);
  }


  private static Collection<? extends Breakpoint> readSdkBreakpoints(JavascriptVm javascriptVm) {
    class CallbackImpl implements JavascriptVm.ListBreakpointsCallback {
      public void failure(Exception exception) {
        problem = exception;
      }

      public void success(Collection<? extends Breakpoint> breakpoints) {
        result = breakpoints;
      }
      Collection<? extends Breakpoint> getResult() {
        if (problem != null) {
          throw new RuntimeException("Failed to synchronize breakpoints", problem); //$NON-NLS-1$
        }
        return result;
      }
      Exception problem = null;
      Collection<? extends Breakpoint> result = null;
    }

    CallbackImpl callback = new CallbackImpl();
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();

    RelayOk relayOk = javascriptVm.listBreakpoints(callback, callbackSemaphore);
    boolean res = callbackSemaphore.tryAcquireDefault(relayOk);
    if (!res) {
      throw new RuntimeException("Timeout"); //$NON-NLS-1$
    }

    return callback.getResult();
  }

  // We need this method to return Set for future purposes.
  private Set<WrappedBreakpoint> getUiBreakpoints() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    Set<WrappedBreakpoint> result = new HashSet<WrappedBreakpoint>();

    BreakpointWrapManager wrapManager = ChromiumDebugPlugin.getDefault().getBreakpointWrapManager();

    for (JavaScriptBreakpointAdapter adapter : wrapManager.getAdapters()) {
      for (IBreakpoint breakpoint : breakpointManager.getBreakpoints(adapter.getModelId())) {
        WrappedBreakpoint breakpointWrapper = adapter.tryWrapBreakpoint(breakpoint);
        if (breakpointWrapper == null) {
          continue;
        }
        result.add(breakpointWrapper);
      }
    }
    return result;
  }

  public static class ProtocolNotSupportedOnRemote extends Exception {
    ProtocolNotSupportedOnRemote() {
    }
    ProtocolNotSupportedOnRemote(String message, Throwable cause) {
      super(message, cause);
    }
    ProtocolNotSupportedOnRemote(String message) {
      super(message);
    }
    ProtocolNotSupportedOnRemote(Throwable cause) {
      super(cause);
    }
  }
}
