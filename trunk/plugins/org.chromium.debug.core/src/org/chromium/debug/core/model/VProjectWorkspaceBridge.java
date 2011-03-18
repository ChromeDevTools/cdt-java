// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.ChromiumSourceDirector;
import org.chromium.debug.core.model.BreakpointSynchronizer.Callback;
import org.chromium.debug.core.model.VmResource.Metadata;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.ExceptionCatchType;
import org.chromium.sdk.JavascriptVm.ScriptsCallback;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.osgi.util.NLS;

/**
 * Virtual project-supporting implementation of {@link WorkspaceBridge}.
 */
public class VProjectWorkspaceBridge implements WorkspaceBridge {
  /** The debug model ID. */
  public static final String DEBUG_MODEL_ID = "org.chromium.debug"; //$NON-NLS-1$

  public static class FactoryImpl implements Factory {
    private final String projectNameBase;

    public FactoryImpl(String projectNameBase) {
      this.projectNameBase = projectNameBase;
    }

    public WorkspaceBridge attachedToVm(RunningTargetData runningTargetData,
        JavascriptVm javascriptVm) {
      // We might want to add URL or something to project name.
      return new VProjectWorkspaceBridge(projectNameBase, runningTargetData, javascriptVm);
    }

    public String getDebugModelIdentifier() {
      return DEBUG_MODEL_ID;
    }

    public JsLabelProvider getLabelProvider() {
      return LABEL_PROVIDER;
    }
  }

  private final IProject debugProject;
  private final JavascriptVm javascriptVm;
  private final ResourceManager resourceManager;
  private final RunningTargetData runningTargetData;
  private final BreakpointMap.InTargetMap breakpointInTargetMap = new BreakpointMap.InTargetMap();
  private final ChromiumSourceDirector sourceDirector;

  public VProjectWorkspaceBridge(String projectName, RunningTargetData runningTargetData,
      JavascriptVm javascriptVm) {
    this.runningTargetData = runningTargetData;
    this.javascriptVm = javascriptVm;
    this.debugProject = ChromiumDebugPluginUtil.createEmptyProject(projectName);
    this.resourceManager = new ResourceManager(debugProject);

    ILaunch launch = runningTargetData.getDebugTarget().getLaunch();

    sourceDirector = (ChromiumSourceDirector) launch.getSourceLocator();
    sourceDirector.initializeVProjectContainers(debugProject, resourceManager);
  }

  public BreakpointSynchronizer getBreakpointSynchronizer() {
    return new BreakpointSynchronizer(javascriptVm,
        breakpointInTargetMap, sourceDirector, breakpointHandler, DEBUG_MODEL_ID);
  }

  public void synchronizeBreakpoints(BreakpointSynchronizer.Direction direction,
      Callback callback) {
    getBreakpointSynchronizer().syncBreakpoints(direction, callback);
  }

  public void startInitialization() {
    LaunchInitializationProcedure.startAsync(this);
  }

  public void launchRemoved() {
    if (debugProject != null) {
      ChromiumDebugPluginUtil.deleteVirtualProjectAsync(debugProject);
    }
  }

  public void beforeDetach() {
  }

  public void handleVmResetEvent() {
    resourceManager.clear();
  }

  public void scriptLoaded(Script newScript) {
    resourceManager.addScript(newScript);
  }

  public void scriptCollected(Script script) {
    resourceManager.scriptCollected(script);
  }

  public void reloadScriptsAtStart() {
    javascriptVm.getScripts(new ScriptsCallback() {
      public void failure(String errorMessage) {
        ChromiumDebugPlugin.logError(errorMessage);
      }

      public void success(Collection<Script> scripts) {
        if (!javascriptVm.isAttached()) {
          return;
        }
        for (Script script : scripts) {
          resourceManager.addScript(script);
        }
      }
    });
  }

  public VmResource findVmResourceFromWorkspaceFile(IFile resource) throws CoreException {
    VmResourceId id = findVmResourceIdFromWorkspaceFile(resource);
    if (id == null) {
      return null;
    }
    return resourceManager.getVmResource(id);
  }

  public VmResource getVProjectVmResource(IFile file) {
    VmResourceId resourceId = resourceManager.getResourceId(file);
    if (resourceId == null) {
      return null;
    }
    return resourceManager.getVmResource(resourceId);
  }

  public VmResource createTemporaryFile(Metadata metadata,
      String proposedFileName) {
    return resourceManager.createTemporaryFile(metadata, proposedFileName);
  }

  private VmResourceId findVmResourceIdFromWorkspaceFile(IFile resource) throws CoreException {
    return sourceDirector.getReverseSourceLookup().findVmResource(resource);
  }

  public void reloadScript(Script script) {
    resourceManager.reloadScript(script);
  }

  public BreakpointHandler getBreakpointHandler() {
    return breakpointHandler;
  }

  private final BreakpointHandlerImpl breakpointHandler = new BreakpointHandlerImpl();

  private class BreakpointHandlerImpl implements BreakpointHandler,
      BreakpointSynchronizer.BreakpointHelper {

    private final Map<JavascriptVm.ExceptionCatchType, Boolean> breakExceptionState =
        Collections.synchronizedMap(new EnumMap<JavascriptVm.ExceptionCatchType, Boolean>(
            JavascriptVm.ExceptionCatchType.class));

    private final EnablementMonitor enablementMonitor = new EnablementMonitor();

    private class EnablementMonitor {
      synchronized void init(IBreakpointManager breakpointManager) {
        sendRequest(breakpointManager.isEnabled());
      }
      synchronized void setState(boolean enabled) {
        sendRequest(enabled);
      }
      private void sendRequest(boolean enabled) {
        javascriptVm.enableBreakpoints(enabled, null, null);
      }
    }

    public void initBreakpointManagerListenerState(IBreakpointManager breakpointManager) {
      enablementMonitor.init(breakpointManager);
    }

    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
      return tryCastBreakpoint(breakpoint) != null;
    }

    public WrappedBreakpoint tryCastBreakpoint(IBreakpoint breakpoint) {
      if (runningTargetData.getDebugTarget().isDisconnected()) {
        return null;
      }
      return ChromiumDebugPlugin.getDefault().getBreakpointWrapManager().wrap(breakpoint);
    }

    public void breakpointAdded(IBreakpoint breakpoint) {
      WrappedBreakpoint lineBreakpoint = tryCastBreakpoint(breakpoint);
      if (lineBreakpoint == null) {
        return;
      }
      if (ChromiumLineBreakpoint.getIgnoreList().contains(lineBreakpoint)) {
        return;
      }
      boolean enabled;
      try {
        enabled = lineBreakpoint.getInner().isEnabled();
      } catch (CoreException e) {
        throw new RuntimeException(e);
      }
      if (!enabled) {
        return;
      }
      IFile file = (IFile) lineBreakpoint.getInner().getMarker().getResource();
      VmResourceId vmResourceId;
      try {
        vmResourceId = findVmResourceIdFromWorkspaceFile(file);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(
            new Exception("Failed to resolve script for the file " + file, e)); //$NON-NLS-1$
        return;
      }
      if (vmResourceId == null) {
        // Might be a script from a different debug target
        return;
      }

      createBreakpointOnRemote(lineBreakpoint, vmResourceId, null, null);
    }

    public void createBreakpointOnRemote(final WrappedBreakpoint lineBreakpoint,
        final VmResourceId vmResourceId,
        final CreateCallback createCallback, SyncCallback syncCallback) {
      ChromiumLineBreakpoint.Helper.CreateOnRemoveCallback callback =
          new ChromiumLineBreakpoint.Helper.CreateOnRemoveCallback() {
        public void success(Breakpoint breakpoint) {
          breakpointInTargetMap.add(breakpoint, lineBreakpoint);
          if (createCallback != null) {
            createCallback.success();
          }
        }
        public void failure(String errorMessage) {
          if (createCallback == null) {
            ChromiumDebugPlugin.logError(errorMessage);
          } else {
            createCallback.failure(new Exception(errorMessage));
          }
        }
      };
      try {
        ChromiumLineBreakpoint.Helper.createOnRemote(lineBreakpoint, vmResourceId,
            runningTargetData, callback, syncCallback);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to create breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(), e));
      }
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
      WrappedBreakpoint lineBreakpoint = tryCastBreakpoint(breakpoint);
      if (lineBreakpoint == null) {
        return;
      }
      if (ChromiumLineBreakpoint.getIgnoreList().contains(lineBreakpoint)) {
        return;
      }
      Breakpoint sdkBreakpoint = breakpointInTargetMap.getSdkBreakpoint(lineBreakpoint);
      if (sdkBreakpoint == null) {
        return;
      }

      try {
        ChromiumLineBreakpoint.Helper.updateOnRemote(sdkBreakpoint, lineBreakpoint);
      } catch (RuntimeException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to change breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(), e));
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to change breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(), e));
      }

    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
      WrappedBreakpoint lineBreakpoint = tryCastBreakpoint(breakpoint);
      if (lineBreakpoint == null) {
        return;
      }
      if (ChromiumLineBreakpoint.getIgnoreList().contains(lineBreakpoint)) {
        return;
      }

      Breakpoint sdkBreakpoint = breakpointInTargetMap.getSdkBreakpoint(lineBreakpoint);
      if (sdkBreakpoint == null) {
        return;
      }

      try {
        if (!breakpoint.isEnabled()) {
          return;
        }
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(e);
        return;
      }
      JavascriptVm.BreakpointCallback callback = new JavascriptVm.BreakpointCallback() {
        public void failure(String errorMessage) {
          ChromiumDebugPlugin.log(new Exception("Failed to remove breakpoint in " + //$NON-NLS-1$
              getTargetNameSafe() + ": " + errorMessage)); //$NON-NLS-1$
        }
        public void success(Breakpoint breakpoint) {
        }
      };
      try {
        sdkBreakpoint.clear(callback, null);
      } catch (RuntimeException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to remove breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(), e));
      }
      breakpointInTargetMap.remove(lineBreakpoint);
    }

    public synchronized void breakpointManagerEnablementChanged(boolean enabled) {
      enablementMonitor.setState(enabled);
    }

    public void breakpointsHit(Collection<? extends Breakpoint> breakpointsHit) {
      if (breakpointsHit.isEmpty()) {
        return;
      }

      for (Breakpoint sdkBreakpoint : breakpointsHit) {
        WrappedBreakpoint uiBreakpoint = breakpointInTargetMap.getUiBreakpoint(sdkBreakpoint);
        if (uiBreakpoint != null) {
          try {
            uiBreakpoint.setIgnoreCount(-1); // reset ignore count as we've hit it
          } catch (CoreException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    private String getTargetNameSafe() {
      try {
        return runningTargetData.getDebugTarget().getLaunch().getLaunchConfiguration().getName();
      } catch (RuntimeException e) {
        return "<unknown>"; //$NON-NLS-1$
      }
    }

    public void readBreakExceptionStateFromRemote() {
      setBreakExceptionStateImpl(ExceptionCatchType.CAUGHT, null);
      setBreakExceptionStateImpl(ExceptionCatchType.UNCAUGHT, null);
    }
    public Boolean getBreakExceptionState(ExceptionCatchType catchType) {
      return breakExceptionState.get(catchType);
    }

    public void setBreakExceptionState(ExceptionCatchType catchType, boolean value) {
      setBreakExceptionStateImpl(catchType, Boolean.valueOf(value));
    }
    private void setBreakExceptionStateImpl(final ExceptionCatchType catchType, Boolean value) {
      JavascriptVm.GenericCallback<Boolean> callback =
          new JavascriptVm.GenericCallback<Boolean>() {
            public void success(Boolean newValue) {
              breakExceptionState.put(catchType, newValue);
            }
            public void failure(Exception exception) {
              ChromiumDebugPlugin.log(new Exception(
                  "Failed to set 'break on exception' value", exception));
            }
          };
      javascriptVm.setBreakOnException(catchType, value, callback, null);
    }


  }

  private final static JsLabelProvider LABEL_PROVIDER = new JsLabelProvider() {
    public String getTargetLabel(DebugTargetImpl debugTarget) {
      String name = debugTarget.getName();
      String status = debugTarget.getVmStatus();
      if (status == null) {
        return name;
      } else {
        return NLS.bind(Messages.DebugTargetImpl_TARGET_NAME_PATTERN, name, status);
      }
    }

    public String getThreadLabel(JavascriptThread thread) {
      String url = thread.getRunningData().getJavascriptEmbedder().getThreadName();
      return NLS.bind(Messages.JsThread_ThreadLabelFormat,
          getThreadStateLabel(thread),
          (url.length() > 0 ? (" : " + url) : "")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String getThreadStateLabel(JavascriptThread thread) {
      DebugContext context;
      if (thread.isSuspended()) {
        // Theoretically the context may be null.
        context = thread.getRunningData().getDebugContext();
      } else {
        context = null;
      }
      if (context == null) {
        return Messages.JsThread_ThreadLabelRunning;
      } else {
        ExceptionData exceptionData = context.getExceptionData();
        if (exceptionData != null) {
          return NLS.bind(Messages.JsThread_ThreadLabelSuspendedExceptionFormat,
              exceptionData.getExceptionMessage());
        } else {
          return Messages.JsThread_ThreadLabelSuspended;
        }
      }
    }

    public String getStackFrameLabel(StackFrame stackFrame) throws DebugException {
      CallFrame callFrame = stackFrame.getCallFrame();
      String name = callFrame.getFunctionName();
      Script script = callFrame.getScript();
      String scriptName;
      if (script == null) {
        scriptName = Messages.StackFrame_UnknownScriptName;
      } else {
        scriptName = VmResourceId.forScript(script).getEclipseSourceName();
      }
      int line = stackFrame.getLineNumber();
      if (line != -1) {
        name = NLS.bind(Messages.StackFrame_NameFormat,
            new Object[] {name, scriptName, line});
      }
      return name;
    }
  };

  public RunningTargetData getRunningTargetData() {
    return runningTargetData;
  }
}
