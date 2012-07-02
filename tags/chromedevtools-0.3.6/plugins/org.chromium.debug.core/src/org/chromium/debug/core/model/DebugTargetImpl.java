// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.util.Destructable;
import org.chromium.sdk.util.DestructingGuard;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;

/**
 * An IDebugTarget implementation for remote JavaScript debugging.
 * This class is essentially a thin wrapper that uses its internal state object
 * as implementation. The at first target is in 'initialize' state, later
 * it should transfer into 'normal' state.
 */

public class DebugTargetImpl extends DebugElementImpl implements IDebugTarget {
  /**
   * Loads browser tabs, consults the {@code selector} which of the tabs to
   * attach to, and if any has been selected, requests an attachment to the tab.
   *
   * @param debugTargetImpl target that is attached
   * @param remoteServer embedding application we are connected with
   * @param destructingGuard guard that should gain any destructable value -- a caller
   *      will dispose everything if this method fails
   * @param attachCallback to invoke on successful attachment, can fail to be called
   * @param monitor to report the progress to
   * @return false if user canceled attach (via tab selection dialog) or true otherwise
   */
  public static boolean attach(DebugTargetImpl debugTargetImpl,
      JavascriptVmEmbedder.ConnectionToRemote remoteServer,
      DestructingGuard destructingGuard, Runnable attachCallback,
      IProgressMonitor monitor) throws CoreException {

    monitor.beginTask("", 2); //$NON-NLS-1$
    JavascriptVmEmbedder.VmConnector connector = remoteServer.selectVm();
    if (connector == null) {
      return false;
    }

    monitor.worked(1);

    ConnectedTargetData.TargetInnerState connectedState;
    ConnectedTargetData connectedData;

    ListenerBlock listenerBlock = new ListenerBlock();
    try {
      connectedState = ConnectedTargetData.create(debugTargetImpl, listenerBlock);
      connectedData = connectedState.getConnectedTargetData();

      final JavascriptVmEmbedder embedder = connector.attach(connectedData.getEmbedderListener(),
              connectedData.getDebugEventListener());
      // From this moment V8 may call our listeners. We block them by listenerBlock for a while.

      Destructable embedderDestructor = new Destructable() {
        public void destruct() {
          embedder.getJavascriptVm().detach();
        }
      };

      destructingGuard.addValue(embedderDestructor);

      connectedData.setVmEmbedder(embedder);

      debugTargetImpl.setInnerState(connectedState);

      connectedData.fireBecameConnectedEvents();

      listenerBlock.setProperlyInitialized();
    } finally {
      listenerBlock.unblock();
    }

    connectedData.initListeners();

    try {
      if (attachCallback != null) {
        attachCallback.run();
      }
    } catch (Exception e) {
      ChromiumDebugPlugin.log(e);
    }

    return true;
  }

  /**
   * Defines an actual state of target. It is who implements virtually all operations
   * of {@link DebugTargetImpl}.
   */
  static abstract class State {
    abstract ITerminate getTerminate();
    abstract ISuspendResume getSuspendResume();
    abstract IDisconnect getDisconnect();
    abstract IBreakpointListener getBreakpointListner();
    abstract IThread[] getThreads() throws DebugException;
    abstract String getName();
    abstract String getVmStatus();
    abstract boolean supportsBreakpoint(IBreakpoint breakpoint);
    abstract EvaluateContext getEvaluateContext();
    abstract ConnectedTargetData getConnectedTargetDataOrNull();
  }

  static final IThread[] EMPTY_THREADS = new IThread[0];

  private final WorkspaceBridge.Factory workspaceBridgeFactory;

  private final SourceWrapSupport sourceWrapSupport;

  private final ILaunch launch;
  private volatile State currentState = new TargetInitializeState(this);

  public DebugTargetImpl(ILaunch launch, WorkspaceBridge.Factory workspaceBridgeFactory,
      SourceWrapSupport sourceWrapSupport) {
    this.launch = launch;
    this.workspaceBridgeFactory = workspaceBridgeFactory;
    this.sourceWrapSupport = sourceWrapSupport;
  }

  public void fireTargetCreated() {
    fireDebugEvent(new DebugEvent(this, DebugEvent.CREATE));
  }

  void setInnerState(State state) {
    currentState = state;
  }

  ConnectedTargetData getConnectedDataOrNull() {
    return currentState.getConnectedTargetDataOrNull();
  }

  WorkspaceBridge.Factory getWorkspaceBridgeFactory() {
    return workspaceBridgeFactory;
  }

  @Override
  public DebugTargetImpl getDebugTarget() {
    return this;
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public boolean canTerminate() {
    return currentState.getTerminate().canTerminate();
  }

  @Override
  public boolean isTerminated() {
    return currentState.getTerminate().isTerminated();
  }

  @Override
  public void terminate() throws DebugException {
    currentState.getTerminate().terminate();
  }

  @Override
  public boolean canResume() {
    return currentState.getSuspendResume().canResume();
  }

  @Override
  public boolean canSuspend() {
    return currentState.getSuspendResume().canSuspend();
  }

  @Override
  public boolean isSuspended() {
    return currentState.getSuspendResume().isSuspended();
  }

  @Override
  public void resume() throws DebugException {
    currentState.getSuspendResume().resume();
  }

  @Override
  public void suspend() throws DebugException {
    currentState.getSuspendResume().suspend();
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    currentState.getBreakpointListner().breakpointAdded(breakpoint);
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    currentState.getBreakpointListner().breakpointRemoved(breakpoint, delta);
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    currentState.getBreakpointListner().breakpointChanged(breakpoint, delta);
  }

  @Override
  public boolean canDisconnect() {
    return currentState.getDisconnect().canDisconnect();
  }

  @Override
  public void disconnect() throws DebugException {
    currentState.getDisconnect().disconnect();
  }

  @Override
  public boolean isDisconnected() {
    return currentState.getDisconnect().isDisconnected();
  }

  @Override
  public boolean supportsStorageRetrieval() {
    return false;
  }

  @Override
  public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
    return null;
  }

  @Override
  public IProcess getProcess() {
    return null;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    return currentState.getThreads();
  }

  @Override
  public boolean hasThreads() throws DebugException {
    return getThreads().length != 0;
  }

  @Override
  public String getName() {
    return currentState.getName();
  }

  @Override
  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return currentState.supportsBreakpoint(breakpoint);
  }

  public String getChromiumModelIdentifier() {
    return workspaceBridgeFactory.getDebugModelIdentifier();
  }

  public WorkspaceBridge.JsLabelProvider getLabelProvider() {
    return workspaceBridgeFactory.getLabelProvider();
  }

  public String getVmStatus() {
    return currentState.getVmStatus();
  }

  public ConnectedTargetData getConnectedOrNull() {
    return currentState.getConnectedTargetDataOrNull();
  }

  public SourceWrapSupport getSourceWrapSupport() {
    return sourceWrapSupport;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter) {
    if (adapter == EvaluateContext.class) {
      return currentState.getEvaluateContext();
    } else if (adapter == ILaunch.class) {
      return this.launch;
    }
    return super.getAdapter(adapter);
  }

  public static List<ConnectedTargetData> getAllConnectedTargetDatas() {
    IDebugTarget[] array = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
    List<ConnectedTargetData> result = new ArrayList<ConnectedTargetData>(array.length);
    for (IDebugTarget target : array) {
      if (target instanceof DebugTargetImpl == false) {
        continue;
      }
      if (target.getLaunch().isTerminated()) {
        continue;
      }
      DebugTargetImpl debugTargetImpl = (DebugTargetImpl) target;

      ConnectedTargetData connectedData = debugTargetImpl.getConnectedDataOrNull();

      if (connectedData == null) {
        continue;
      }

      result.add(connectedData);
    }
    return result;
  }

  static class ListenerBlock {
    private volatile boolean isBlocked = true;
    private volatile boolean hasBeenProperlyInitialized = false;
    private final Object monitor = new Object();
    void waitUntilReady() {
      if (isBlocked) {
        synchronized (monitor) {
          while (isBlocked) {
            try {
              monitor.wait();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
      if (!hasBeenProperlyInitialized) {
        throw new RuntimeException("DebugTarget has not been properly initialized"); //$NON-NLS-1$
      }
    }
    void setProperlyInitialized() {
      hasBeenProperlyInitialized = true;
    }
    void unblock() {
      isBlocked = false;
      synchronized (monitor) {
        monitor.notifyAll();
      }
    }
  }

  /**
   * Fires a debug event
   *
   * @param event to be fired
   */
  public static void fireDebugEvent(DebugEvent event) {
    DebugPlugin debugPlugin = DebugPlugin.getDefault();
    if (debugPlugin != null) {
      debugPlugin.fireDebugEventSet(new DebugEvent[] { event });
    }
  }
}
