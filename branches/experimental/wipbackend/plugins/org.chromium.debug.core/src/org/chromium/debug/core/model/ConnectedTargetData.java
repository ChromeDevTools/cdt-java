// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugTargetImpl.ListenerBlock;
import org.chromium.debug.core.model.DebugTargetImpl.State;
import org.chromium.debug.core.sourcemap.PositionMapBuilderImpl;
import org.chromium.debug.core.sourcemap.SourcePositionMap;
import org.chromium.debug.core.sourcemap.SourcePositionMapBuilder;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.osgi.util.NLS;

/**
 * Contains state and behavior of 'connected' {@link DebugTargetImpl}. Its inner implementation of
 * {@link DebugTargetImpl#State} is for {@link DebugTargetImpl} only and is available externally
 * only at construction. The class can be used
 * only after {@link #setVmEmbedder} was called. Listeners can be used from the
 * beginning, but they are blocking until {@link #listenerBlock} is unblocked which is done
 * externally after {@link #setVmEmbedder} is called.
 * <p>
 * It corresponds to 'connected' state of target and post-terminated state.
 */
public class ConnectedTargetData {

  /**
   * Creates instance and returns its inner state class. This is the only moment when inner
   * state class gets available outside the class.
   * @param listenerBlock blocks 2 event listeners to let all infrastructure get initialized
   * @return inner state of data that provides getter to the data itself.
   */
  static TargetInnerState create(DebugTargetImpl debugTargetImpl, ListenerBlock listenerBlock) {
    ConnectedTargetData data = new ConnectedTargetData(debugTargetImpl, listenerBlock);

    // No public getter for stateImpl. We expose state only to one who creates us.
    return data.debugTargetState;
  }

  private final DebugTargetImpl debugTargetImpl;
  private final TargetInnerState debugTargetState = new TargetInnerState();
  private final JavascriptThread singleThread;
  private final JavascriptThread[] threadArray;
  private final SourcePositionMapBuilder sourcePositionMapBuilder = new PositionMapBuilderImpl();
  private final ListenerBlock listenerBlock;
  private final DebugEventListenerImpl debugEventListener = new DebugEventListenerImpl();

  private JavascriptVmEmbedder vmEmbedder = null;
  private WorkspaceBridge workspaceRelations = null;

  private volatile boolean isDisconnected = false;

  private ConnectedTargetData(DebugTargetImpl debugTargetImpl, ListenerBlock listenerBlock) {
    this.debugTargetImpl = debugTargetImpl;
    this.singleThread = new JavascriptThread(this);
    this.threadArray = new JavascriptThread[] { singleThread };
    this.listenerBlock = listenerBlock;
  }

  void setVmEmbedder(JavascriptVmEmbedder vmEmbedder) {
    ConnectedTargetData.this.vmEmbedder = vmEmbedder;

    initWorkspaceRelations();
  }

  public void initListeners() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    breakpointManager.addBreakpointListener(debugTargetState.getBreakpointListner());
    breakpointManager.addBreakpointManagerListener(workspaceRelations.getBreakpointHandler());
    workspaceRelations.getBreakpointHandler().initBreakpointManagerListenerState(breakpointManager);
    workspaceRelations.getBreakpointHandler().readBreakExceptionStateFromRemote();

    workspaceRelations.startInitialization();
  }

  public JavascriptVmEmbedder getJavascriptEmbedder() {
    return vmEmbedder;
  }

  public JavascriptVm getJavascriptVm() {
    return getJavascriptEmbedder().getJavascriptVm();
  }

  void fireBecameConnectedEvents() {
    setDisconnected(false);
    DebugTargetImpl.fireDebugEvent(new DebugEvent(debugTargetImpl, DebugEvent.CHANGE));
    fireEventForThread(DebugEvent.CREATE, DebugEvent.UNSPECIFIED);
  }

  void fireResumeEvent(int detail) {
    fireEventForThread(DebugEvent.RESUME, detail);
    DebugTargetImpl.fireDebugEvent(new DebugEvent(debugTargetImpl, DebugEvent.RESUME, detail));
  }

  public Collection<? extends VmResource> getVmResource(IFile resource) throws CoreException {
    return workspaceRelations.findVmResourcesFromWorkspaceFile(resource);
  }

  private final VmStatusListenerImpl vmStatusListener = new VmStatusListenerImpl();

  private class VmStatusListenerImpl implements DebugEventListener.VmStatusListener {
    private String currentRequest = null;
    private int numberOfEnqueued;

    public synchronized void busyStatusChanged(String currentRequest, int numberOfEnqueued) {
      this.currentRequest = currentRequest;
      this.numberOfEnqueued = numberOfEnqueued;
      DebugTargetImpl.fireDebugEvent(new DebugEvent(debugTargetImpl, DebugEvent.CHANGE));
    }

    public synchronized String getStatusString() {
      if (currentRequest == null) {
        return null;
      }
      return NLS.bind(Messages.DebugTargetImpl_BUSY_WITH, currentRequest, numberOfEnqueued);
    }
  }

  public void synchronizeBreakpoints(BreakpointSynchronizer.Direction direction,
      BreakpointSynchronizer.Callback callback) {
    workspaceRelations.synchronizeBreakpoints(direction, callback);
  }

  public DebugEventListenerImpl getDebugEventListener() {
    return debugEventListener;
  }

  public JavascriptVmEmbedder.Listener getEmbedderListener() {
    return embedderListener;
  }

  public WorkspaceBridge getWorkspaceRelations() {
    return workspaceRelations;
  }

  public SourcePositionMap getSourcePositionMap() {
    return sourcePositionMapBuilder.getSourcePositionMap();
  }

  public SourcePositionMapBuilder getSourcePositionMapBuilder() {
    return sourcePositionMapBuilder;
  }

  public DebugTargetImpl getDebugTarget() {
    return debugTargetImpl;
  }

  public String getName() {
    return debugTargetState.getName();
  }

  private JavascriptThread getThread() {
    return disconnectAspect.isDisconnected()
        ? null
        : singleThread;
  }

  private void fireEventForThread(int kind, int detail) {
    try {
      IThread[] threads = debugTargetState.getThreads();
      if (threads.length > 0) {
        DebugTargetImpl.fireDebugEvent(new DebugEvent(threads[0], kind, detail));
      }
    } catch (DebugException e) {
      // Actually, this is not thrown in our getThreads()
      return;
    }
  }

  private void fireTerminateEvent() {
    // TODO(peter.rybin): from Alexander Pavlov: I think you need to fire a terminate event after
    // this line, for consolePseudoProcess if one is not null.

    // Do not report on threads -- the children are gone when terminated.
    DebugTargetImpl.fireDebugEvent(
        new DebugEvent(debugTargetImpl, DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED));
    DebugTargetImpl.fireDebugEvent(
        new DebugEvent(debugTargetImpl.getLaunch(), DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED));
  }

  void fireSuspendEvent(int detail) {
    fireEventForThread(DebugEvent.SUSPEND, detail);
    DebugTargetImpl.fireDebugEvent(new DebugEvent(debugTargetImpl, DebugEvent.SUSPEND, detail));
  }

  private void setDisconnected(boolean disconnected) {
    isDisconnected = disconnected;
  }

  boolean isDisconnected() {
    return isDisconnected;
  }

  private void initWorkspaceRelations() {
    ConnectedTargetData.this.workspaceRelations =
        debugTargetImpl.getWorkspaceBridgeFactory().attachedToVm(ConnectedTargetData.this,
            vmEmbedder.getJavascriptVm());

    // We'd like to know when launch is removed to remove our project.
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(new ILaunchListener() {
      public void launchAdded(ILaunch launch) {
      }
      public void launchChanged(ILaunch launch) {
      }
      // TODO(peter.rybin): maybe have one instance of listener for all targets?
      public void launchRemoved(ILaunch launch) {
        if (launch != debugTargetImpl.getLaunch()) {
          return;
        }
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
        workspaceRelations.launchRemoved();
      }
    });
  }

  private final IDisconnect disconnectAspect = new IDisconnect() {
    public boolean canDisconnect() {
      return !isDisconnected();
    }

    public void disconnect() throws DebugException {
      if (!canDisconnect()) {
        return;
      }
      workspaceRelations.beforeDetach();
      if (!vmEmbedder.getJavascriptVm().detach()) {
        ChromiumDebugPlugin.logWarning(Messages.DebugTargetImpl_BadResultWhileDisconnecting);
      }
      // This is a duplicated call to disconnected().
      // The primary one comes from V8DebuggerToolHandler#onDebuggerDetached
      // but we want to make sure the target becomes disconnected even if
      // there is a browser failure and it does not respond.
      debugEventListener.disconnected();
    }

    public boolean isDisconnected() {
      return isDisconnected;
    }
  };

  private final ITerminate terminateAspect = new ITerminate() {
    public boolean canTerminate() {
      return !isTerminated();
    }

    public boolean isTerminated() {
      return disconnectAspect.isDisconnected();
    }

    public void terminate() throws DebugException {
      disconnectAspect.disconnect();
    }
  };

  private final JavascriptVmEmbedder.Listener embedderListener =
      new JavascriptVmEmbedder.Listener() {
    public void reset() {
      listenerBlock.waitUntilReady();
      workspaceRelations.handleVmResetEvent();
      DebugTargetImpl.fireDebugEvent(
          new DebugEvent(debugTargetImpl, DebugEvent.CHANGE, DebugEvent.CONTENT));
    }
    public void closed() {
      debugEventListener.disconnected();
    }
  };

  private class DebugEventListenerImpl implements DebugEventListener {

    public void disconnected() {
      if (!disconnectAspect.isDisconnected()) {
        setDisconnected(true);
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointManagerListener(
            workspaceRelations.getBreakpointHandler());
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(
            debugTargetImpl);
        fireTerminateEvent();
      }
    }

    public void resumed() {
      listenerBlock.waitUntilReady();
      singleThread.getRemoteEventListener().resumed(null);
    }

    public void suspended(DebugContext context) {
      listenerBlock.waitUntilReady();
      singleThread.getRemoteEventListener().suspended(context);
    }

    public void scriptLoaded(Script newScript) {
      listenerBlock.waitUntilReady();
      workspaceRelations.scriptLoaded(newScript);
    }

    public void scriptCollected(Script script) {
      listenerBlock.waitUntilReady();
      workspaceRelations.scriptCollected(script);
    }

    public void scriptContentChanged(Script newScript) {
      listenerBlock.waitUntilReady();
      workspaceRelations.reloadScript(newScript);
    }

    public VmStatusListener getVmStatusListener() {
      return vmStatusListener;
    }
  }

  class TargetInnerState extends State {
    @Override
    boolean supportsBreakpoint(IBreakpoint breakpoint) {
      return workspaceRelations.getBreakpointHandler().supportsBreakpoint(breakpoint);
    }

    @Override
    String getVmStatus() {
      if (isDisconnected) {
        return null;
      }
      return vmStatusListener.getStatusString();
    }

    @Override
    IThread[] getThreads() throws DebugException {
      return disconnectAspect.isDisconnected()
          ? DebugTargetImpl.EMPTY_THREADS
          : threadArray;
    }

    @Override
    ConnectedTargetData getConnectedTargetDataOrNull() {
      return getConnectedTargetData();
    }

    ConnectedTargetData getConnectedTargetData() {
      return ConnectedTargetData.this;
    }

    @Override
    ISuspendResume getSuspendResume() {
      return singleThread.getSuspendResumeAspect();
    }

    @Override
    ITerminate getTerminate() {
      return terminateAspect;
    }

    @Override
    IDisconnect getDisconnect() {
      return disconnectAspect;
    }

    @Override
    String getName() {
      JavascriptVmEmbedder vmEmbedder = getJavascriptEmbedder();
      return vmEmbedder.getTargetName();
    }

    @Override
    EvaluateContext getEvaluateContext() {
      return getThread().getEvaluateContext();
    }

    @Override
    IBreakpointListener getBreakpointListner() {
      return workspaceRelations.getBreakpointHandler();
    }
  }
}