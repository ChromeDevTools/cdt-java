// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.ChromiumSourceDirector;
import org.chromium.debug.core.ScriptNameManipulator.ScriptNamePattern;
import org.chromium.debug.core.model.BreakpointSynchronizer.Callback;
import org.chromium.debug.core.model.ChromiumLineBreakpoint.MutableProperty;
import org.chromium.debug.core.model.VmResource.Metadata;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.JavaScriptRegExpSupport;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.ExceptionCatchMode;
import org.chromium.sdk.JavascriptVm.ScriptsCallback;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.GenericCallback;
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

    public WorkspaceBridge attachedToVm(ConnectedTargetData connectedTargetData,
        JavascriptVm javascriptVm) {
      // We might want to add URL or something to project name.
      return new VProjectWorkspaceBridge(projectNameBase, connectedTargetData, javascriptVm);
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
  private final ConnectedTargetData connectedTargetData;
  private final BreakpointMap.InTargetMap breakpointInTargetMap = new BreakpointMap.InTargetMap();
  private final ChromiumSourceDirector sourceDirector;

  public VProjectWorkspaceBridge(String projectName, ConnectedTargetData connectedTargetData,
      JavascriptVm javascriptVm) {
    this.connectedTargetData = connectedTargetData;
    this.javascriptVm = javascriptVm;
    this.debugProject = ChromiumDebugPluginUtil.createEmptyProject(projectName);
    this.resourceManager = new ResourceManager(debugProject);

    ILaunch launch = connectedTargetData.getDebugTarget().getLaunch();

    sourceDirector = (ChromiumSourceDirector) launch.getSourceLocator();
    sourceDirector.initializeVProjectContainers(debugProject, resourceManager,
        connectedTargetData.getJavascriptEmbedder());
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

  public Collection<? extends VmResource> findVmResourcesFromWorkspaceFile(IFile resource)
      throws CoreException {
    VmResourceRef vmResourceRef = findVmResourceRefFromWorkspaceFile(resource);
    if (vmResourceRef == null) {
      return null;
    }
    return vmResourceRef.accept(RESOLVE_RESOURCE_VISITOR);
  }

  private final VmResourceRef.Visitor<Collection<? extends VmResource>> RESOLVE_RESOURCE_VISITOR =
      new VmResourceRef.Visitor<Collection<? extends VmResource>>() {
    @Override
    public java.util.Collection<? extends VmResource> visitRegExpBased(
        ScriptNamePattern scriptNamePattern) {
      Pattern pattern = JavaScriptRegExpSupport.convertToJavaPattern(scriptNamePattern);
      return resourceManager.findVmResources(pattern);
    }

    @Override
    public Collection<? extends VmResource> visitResourceId(VmResourceId resourceId) {
      return Collections.singletonList(resourceManager.getVmResource(resourceId));
    }
  };

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

  private VmResourceRef findVmResourceRefFromWorkspaceFile(IFile resource) throws CoreException {
    return sourceDirector.findVmResourceRef(resource);
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

    private volatile JavascriptVm.ExceptionCatchMode breakExceptionMode = null;

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

    @Override
    public Breakpoint getSdkBreakpoint(ChromiumLineBreakpoint wrappedBreakpoint) {
      return breakpointInTargetMap.getSdkBreakpoint(wrappedBreakpoint);
    }

    public ChromiumLineBreakpoint tryCastBreakpoint(IBreakpoint breakpoint) {
      if (connectedTargetData.getDebugTarget().isDisconnected()) {
        return null;
      }
      return ChromiumBreakpointAdapter.tryCastBreakpoint(breakpoint);
    }

    public void breakpointAdded(IBreakpoint breakpoint) {
      ChromiumLineBreakpoint lineBreakpoint = tryCastBreakpoint(breakpoint);
      if (lineBreakpoint == null) {
        return;
      }
      if (ChromiumLineBreakpoint.getIgnoreList().contains(lineBreakpoint)) {
        return;
      }
      IFile file = (IFile) lineBreakpoint.getMarker().getResource();
      VmResourceRef vmResourceRef;
      try {
        vmResourceRef = findVmResourceRefFromWorkspaceFile(file);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(
            new Exception("Failed to resolve script for the file " + file, e)); //$NON-NLS-1$
        return;
      }
      if (vmResourceRef == null) {
        // Might be a script from a different debug target
        return;
      }

      try {
        createBreakpointOnRemote(lineBreakpoint, vmResourceRef, null, null);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to create breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(), e));
        throw new RuntimeException(e);
      }
    }

    public RelayOk createBreakpointOnRemote(final ChromiumLineBreakpoint lineBreakpoint,
        final VmResourceRef vmResourceRef,
        final CreateCallback createCallback, SyncCallback syncCallback) throws CoreException {
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
      return ChromiumLineBreakpoint.Helper.createOnRemote(lineBreakpoint, vmResourceRef,
          connectedTargetData, callback, syncCallback);
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
      ChromiumLineBreakpoint lineBreakpoint = tryCastBreakpoint(breakpoint);
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

      Set<MutableProperty> propertyDelta = lineBreakpoint.getChangedProperty(delta);

      if (propertyDelta.isEmpty()) {
        return;
      }

      try {
        ChromiumLineBreakpoint.Helper.updateOnRemote(sdkBreakpoint, lineBreakpoint, propertyDelta);
      } catch (RuntimeException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to change breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(), e));
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to change breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(), e));
      }

    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
      ChromiumLineBreakpoint lineBreakpoint = tryCastBreakpoint(breakpoint);
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

    public Collection<? extends IBreakpoint> breakpointsHit(
        Collection<? extends Breakpoint> breakpointsHit) {
      if (breakpointsHit.isEmpty()) {
        return Collections.emptyList();
      }

      Collection<IBreakpoint> uiBreakpoints = new ArrayList<IBreakpoint>(breakpointsHit.size());

      for (Breakpoint sdkBreakpoint : breakpointsHit) {
        ChromiumLineBreakpoint uiBreakpoint = breakpointInTargetMap.getUiBreakpoint(sdkBreakpoint);
        if (uiBreakpoint != null) {
          try {
            uiBreakpoint.silentlyResetIgnoreCount(); // reset ignore count as we've hit it
          } catch (CoreException e) {
            ChromiumDebugPlugin.log(new Exception("Failed to reset breakpoint ignore count", e));
          }

          uiBreakpoints.add(uiBreakpoint);
        }
      }
      return uiBreakpoints;
    }

    private String getTargetNameSafe() {
      try {
        return connectedTargetData.getDebugTarget().getLaunch().getLaunchConfiguration().getName();
      } catch (RuntimeException e) {
        return "<unknown>"; //$NON-NLS-1$
      }
    }

    @Override
    public void readBreakExceptionStateFromRemote() {
      setBreakExceptionStateImpl(null);
    }

    @Override
    public ExceptionCatchMode getBreakExceptionState() {
      return breakExceptionMode;
    }

    @Override
    public void setBreakExceptionState(ExceptionCatchMode catchMode) {
      setBreakExceptionStateImpl(catchMode);
    }

    private void setBreakExceptionStateImpl(final ExceptionCatchMode catchMode) {
      GenericCallback<ExceptionCatchMode> callback =
          new GenericCallback<ExceptionCatchMode>() {
            public void success(ExceptionCatchMode newValue) {
              breakExceptionMode = newValue;
            }
            public void failure(Exception exception) {
              ChromiumDebugPlugin.log(new Exception(
                  "Failed to set 'break on exception' value", exception));
            }
          };
      javascriptVm.setBreakOnException(catchMode, callback, null);
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
      String url = thread.getConnectedData().getJavascriptEmbedder().getThreadName();
      return NLS.bind(Messages.JsThread_ThreadLabelFormat,
          getThreadStateLabel(thread),
          (url.length() > 0 ? (" : " + url) : "")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String getThreadStateLabel(JavascriptThread thread) {
      return thread.describeState(THREAD_DESCRIBE_VISITOR);
    }

    private final JavascriptThread.StateVisitor<String> THREAD_DESCRIBE_VISITOR =
        new JavascriptThread.StateVisitor<String>() {
          @Override
          public String visitResumed(JavascriptThread.ResumeReason resumeReason) {
            return Messages.JsThread_ThreadLabelRunning;
          }

          @Override
          public String visitSuspended(IBreakpoint[] breakpoints,
              ExceptionData exceptionData) {
            if (exceptionData != null) {
              return NLS.bind(Messages.JsThread_ThreadLabelSuspendedExceptionFormat,
                  exceptionData.getExceptionMessage());
            } else {
              return Messages.JsThread_ThreadLabelSuspended;
            }
          }
        };

    public String getStackFrameLabel(StackFrame stackFrame) throws DebugException {
      CallFrame callFrame = stackFrame.getCallFrame();
      String name = callFrame.getFunctionName();
      Script script = callFrame.getScript();
      String scriptName;
      if (script == null) {
        scriptName = Messages.StackFrame_UnknownScriptName;
      } else {
        scriptName = VmResourceId.forScript(script).getVisibleName();
      }
      int line = stackFrame.getLineNumber();
      if (line != -1) {
        name = NLS.bind(Messages.StackFrame_NameFormat,
            new Object[] {name, scriptName, line});
      }
      return name;
    }
  };

  public ConnectedTargetData getConnectedTargetData() {
    return connectedTargetData;
  }
}
