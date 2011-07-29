// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.containsSafe;
import static org.chromium.sdk.util.BasicUtil.removeSafe;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.ScriptNameManipulator.ScriptNamePattern;
import org.chromium.debug.core.sourcemap.SourcePosition;
import org.chromium.debug.core.sourcemap.SourcePositionMap;
import org.chromium.debug.core.sourcemap.SourcePositionMap.TranslateDirection;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Breakpoint.Target;
import org.chromium.sdk.BreakpointTypeExtension.ScriptRegExpSupport;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.osgi.util.NLS;

/**
 * JavaScript line breakpoint.
 */
public class ChromiumLineBreakpoint extends LineBreakpoint {

  /** Ignore count */
  private static final String IGNORE_COUNT_ATTR = ChromiumDebugPlugin.PLUGIN_ID + ".ignoreCount"; //$NON-NLS-1$

  /** Condition */
  private static final String CONDITION_ATTR = ChromiumDebugPlugin.PLUGIN_ID + ".condition"; //$NON-NLS-1$

  /**
   * Default constructor is required for the breakpoint manager to re-create
   * persisted breakpoints. After instantiating a breakpoint, the setMarker
   * method is called to restore this breakpoint's attributes.
   */
  public ChromiumLineBreakpoint() {
  }

  /**
   * Constructs a line breakpoint on the given resource at the given line number
   * (line number is 1-based).
   *
   * @param resource file on which to set the breakpoint
   * @param lineNumber 1-based line number of the breakpoint
   * @throws CoreException if unable to create the breakpoint
   */
  public ChromiumLineBreakpoint(final IResource resource, final int lineNumber,
      final String modelId) throws CoreException {
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IMarker marker = resource.createMarker(ChromiumDebugPlugin.BP_MARKER);
        setMarker(marker);
        marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        marker.setAttribute(IBreakpoint.ID, modelId);
        marker.setAttribute(IMarker.MESSAGE, NLS.bind(
            Messages.JsLineBreakpoint_MessageMarkerFormat, resource.getName(), lineNumber));
      }
    };
    run(getMarkerRule(resource), runnable);
  }

  @Override
  public boolean isEnabled() {
    try {
      return super.isEnabled();
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
      return false;
    }
  }

  public void setIgnoreCount(int ignoreCount) {
    setMarkerAttribute(IGNORE_COUNT_ATTR, ignoreCount);
  }

  private void setMarkerAttribute(String attributeName, Object value) {
    try {
      setAttribute(attributeName, value);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  public int getIgnoreCount() {
    return getMarker().getAttribute(IGNORE_COUNT_ATTR, Breakpoint.EMPTY_VALUE);
  }

  public void setCondition(String condition) throws CoreException {
    setMarkerAttribute(CONDITION_ATTR, condition);
  }

  public String getCondition() {
    return getMarker().getAttribute(CONDITION_ATTR, null);
  }

  public String getModelIdentifier() {
    return getMarker().getAttribute(IBreakpoint.ID, "");
  }

  /**
   * A helper that propagates changes in Eclipse Debugger breakpoints (i.e.
   * {@link ChromiumLineBreakpoint}) to ChromeDevTools SDK breakpoints. Note that
   * {@link ChromiumLineBreakpoint} can't do it itself, because it may correspond to several
   * SDK {@link JavascriptVm}'s simultaneously.
   */
  public static class Helper {
    // TODO: rename 'remove' -> 'remote'.
    public interface CreateOnRemoveCallback {
      void success(Breakpoint breakpoint);
      void failure(String errorMessage);
    }

    public static RelayOk createOnRemote(final WrappedBreakpoint uiBreakpoint,
        VmResourceRef vmResourceRef, final ConnectedTargetData connectedTargetData,
        final CreateOnRemoveCallback createOnRemoveCallback,
        SyncCallback syncCallback) throws CoreException {
      final JavascriptVm javascriptVm = connectedTargetData.getJavascriptVm();

      // ILineBreakpoint lines are 1-based while V8 lines are 0-based
      final int line = (uiBreakpoint.getInner().getLineNumber() - 1);
      final int column = 0;

      BreakpointCallback callback = new BreakpointCallback() {
        public void success(Breakpoint sdkBreakpoint) {
          createOnRemoveCallback.success(sdkBreakpoint);
        }
        public void failure(String errorMessage) {
          createOnRemoveCallback.failure(errorMessage);
        }
      };

      class SdkParams {
        SdkParams(Target target, int line, int column) {
          this.target = target;
          this.line = line;
          this.column = column;
        }

        final Breakpoint.Target target;
        final int line;
        final int column;
      }

      SdkParams sdkParams = vmResourceRef.accept(new VmResourceRef.Visitor<SdkParams>() {
        @Override
        public SdkParams visitRegExpBased(ScriptNamePattern scriptNamePattern) {
          // TODO: support source mapping perhaps.

          ScriptRegExpSupport scriptRegExpSupport =
              javascriptVm.getBreakpointTypeExtension().getScriptRegExpSupport();
          if (scriptRegExpSupport == null) {
            // TODO: check earlier in UI.
            throw new RuntimeException("Script RegExp is not supported by VM");
          }

          final Breakpoint.Target targetValue =
              scriptRegExpSupport.createTarget(scriptNamePattern.getJavaScriptRegExp());
          return new SdkParams(targetValue, line, column);
        }

        @Override
        public SdkParams visitResourceId(VmResourceId resourceId) {
          SourcePositionMap map = connectedTargetData.getSourcePositionMap();
          SourcePosition vmPosition =
              map.translatePosition(resourceId, line, column,TranslateDirection.USER_TO_VM);
          final int vmLine = vmPosition.getLine();
          final int vmColumn = vmPosition.getColumn();
          final Breakpoint.Target target;
          VmResourceId vmSideVmResourceId = vmPosition.getId();
          if (vmSideVmResourceId.getId() == null) {
            target = new Breakpoint.Target.ScriptName(vmSideVmResourceId.getName());
          } else {
            target = new Breakpoint.Target.ScriptId(vmSideVmResourceId.getId());
          }

          return new SdkParams(target, vmLine, vmColumn);
        }
      });

      return javascriptVm.setBreakpoint(
          sdkParams.target,
          sdkParams.line,
          sdkParams.column,
          uiBreakpoint.getInner().isEnabled(),
          uiBreakpoint.getCondition(),
          uiBreakpoint.getIgnoreCount(),
          callback, syncCallback);
    }

    public static void updateOnRemote(Breakpoint sdkBreakpoint,
        WrappedBreakpoint uiBreakpoint) throws CoreException {
      sdkBreakpoint.setCondition(uiBreakpoint.getCondition());
      sdkBreakpoint.setEnabled(uiBreakpoint.getInner().isEnabled());
      sdkBreakpoint.setIgnoreCount(uiBreakpoint.getIgnoreCount());
      sdkBreakpoint.flush(null, null);
    }

    public static ChromiumLineBreakpoint createLocal(Breakpoint sdkBreakpoint,
        IBreakpointManager breakpointManager, IFile resource, int script_line_offset,
        String debugModelId) throws CoreException {
      ChromiumLineBreakpoint uiBreakpoint = new ChromiumLineBreakpoint(resource,
          (int) sdkBreakpoint.getLineNumber() + 1 + script_line_offset,
          debugModelId);
      uiBreakpoint.setCondition(sdkBreakpoint.getCondition());
      uiBreakpoint.setEnabled(sdkBreakpoint.isEnabled());
      uiBreakpoint.setIgnoreCount(sdkBreakpoint.getIgnoreCount());
      WrappedBreakpoint uiBreakpointWrapper = ChromiumBreakpointAdapter.wrap(uiBreakpoint);
      ignoreList.add(uiBreakpointWrapper);
      try {
        breakpointManager.addBreakpoint(uiBreakpoint);
      } finally {
        ignoreList.remove(uiBreakpointWrapper);
      }
      return uiBreakpoint;
    }
  }

  private static final BreakpointIgnoreList ignoreList = new BreakpointIgnoreList();

  public static BreakpointIgnoreList getIgnoreList() {
    return ignoreList;
  }

  public static class BreakpointIgnoreList {
    private final List<WrappedBreakpoint> list = new ArrayList<WrappedBreakpoint>(1);

    public boolean contains(WrappedBreakpoint breakpoint) {
      return containsSafe(list, breakpoint);
    }

    public void remove(WrappedBreakpoint lineBreakpoint) {
      boolean res = removeSafe(list, lineBreakpoint);
      if (!res) {
        throw new IllegalStateException();
      }
    }

    public void add(WrappedBreakpoint lineBreakpoint) {
      if (containsSafe(list, lineBreakpoint)) {
        throw new IllegalStateException();
      }
      list.add(lineBreakpoint);
    }
  }
}
