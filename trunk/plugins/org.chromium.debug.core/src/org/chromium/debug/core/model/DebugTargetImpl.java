// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.LiveEditDebugEventListener;
import org.chromium.sdk.Script;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.DebugContext.State;
import org.chromium.sdk.DebugContext.StepAction;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.osgi.util.NLS;

/**
 * An IDebugTarget implementation for remote JavaScript debugging.
 * Can debug any target that supports the ChromeDevTools protocol.
 */
public class DebugTargetImpl extends DebugElementImpl implements IDebugTarget {

  private static final IThread[] EMPTY_THREADS = new IThread[0];

  private final ILaunch launch;

  private final JavascriptThread[] threads;

  private JavascriptVmEmbedder vmEmbedder = STUB_VM_EMBEDDER;

  private volatile DebugContext debugContext;

  private boolean isSuspended = false;

  private boolean isDisconnected = false;

  private final WorkspaceBridge.Factory workspaceBridgeFactory;

  private WorkspaceBridge workspaceRelations = null;

  private ListenerBlock listenerBlock = null;

  public DebugTargetImpl(ILaunch launch, WorkspaceBridge.Factory workspaceBridgeFactory) {
    super(null);
    this.workspaceBridgeFactory = workspaceBridgeFactory;
    this.launch = launch;
    this.threads = new JavascriptThread[] { new JavascriptThread(this) };
  }


  /**
   * Loads browser tabs, consults the {@code selector} which of the tabs to
   * attach to, and if any has been selected, requests an attachment to the tab.
   *
   * @param remoteServer embedding application we are connected with
   * @param attachCallback to invoke on successful attachment, can fail to be called
   * @param monitor to report the progress to
   * @return whether the target has attached to a tab
   * @throws CoreException
   */
  public boolean attach(JavascriptVmEmbedder.ConnectionToRemote remoteServer,
      DestructingGuard destructingGuard, Runnable attachCallback,
      IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("", 2); //$NON-NLS-1$
    JavascriptVmEmbedder.VmConnector connector = remoteServer.selectVm();
    if (connector == null) {
      return false;
    }
    monitor.worked(1);
    this.listenerBlock = new ListenerBlock();
    try {
      final JavascriptVmEmbedder embedder = connector.attach(embedderListener, debugEventListener);
      // From this moment V8 may call our listeners. We block them by listenerBlock for a while.

      Destructable embedderDestructor = new Destructable() {
        public void destruct() {
          embedder.getJavascriptVm().detach();
        }
      };

      destructingGuard.addValue(embedderDestructor);

      this.vmEmbedder = embedder;

      // We'd like to know when launch is removed to remove our project.
      DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchListener);

      this.workspaceRelations = workspaceBridgeFactory.attachedToVm(this,
          vmEmbedder.getJavascriptVm());
      listenerBlock.setProperlyInitialized();
    } finally {
      listenerBlock.unblock();
    }

    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    breakpointManager.addBreakpointListener(this);
    breakpointManager.addBreakpointManagerListener(workspaceRelations.getBreakpointHandler());
    workspaceRelations.getBreakpointHandler().initBreakpointManagerListenerState(breakpointManager);

    invokeAttachCallback(attachCallback);

    workspaceRelations.startInitialization();

    return true;
  }

  /**
   * To initialize debug UI we call "resumed" manually first. However, "suspended" event
   * may have already arrived, so we should be careful about this.
   */
  void resumeSessionByDefault() {
    debugEventListener.resumedByDefault();
  }

  private void invokeAttachCallback(final Runnable attachCallback) {
    try {
      if (attachCallback != null) {
        attachCallback.run();
      }
    } finally {
      fireCreationEvent();
    }
  }

  public String getName() {
    JavascriptVmEmbedder vmEmbedder = getJavascriptEmbedder();
    if (vmEmbedder == null) {
      return ""; //$NON-NLS-1$
    }
    return vmEmbedder.getTargetName();
  }

  public String getVmStatus() {
    return vmStatusListener.getStatusString();
  }

  public IProcess getProcess() {
    return null;
  }

  public JavascriptVmEmbedder getJavascriptEmbedder() {
    return vmEmbedder;
  }

  public IThread[] getThreads() throws DebugException {
    return isDisconnected()
        ? EMPTY_THREADS
        : threads;
  }

  public boolean hasThreads() throws DebugException {
    return getThreads().length > 0;
  }

  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return workspaceRelations.getBreakpointHandler().supportsBreakpoint(breakpoint);
  }

  @Override
  public DebugTargetImpl getDebugTarget() {
    return this;
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  public String getChromiumModelIdentifier() {
    return workspaceBridgeFactory.getDebugModelIdentifier();
  }

  public boolean canTerminate() {
    return !isTerminated();
  }

  public boolean isTerminated() {
    return isDisconnected();
  }

  public void terminate() throws DebugException {
    disconnect();
  }

  public boolean canResume() {
    return !isDisconnected() && isSuspended();
  }

  public synchronized boolean isSuspended() {
    return isSuspended;
  }

  private synchronized void setSuspended(boolean isSuspended) {
    this.isSuspended = isSuspended;
  }

  public void suspended(int detail) {
    setSuspended(true);
    getThread().reset();
    fireSuspendEvent(detail);
  }

  public void resume() throws DebugException {
    debugContext.continueVm(StepAction.CONTINUE, 1, null);
    // Let's pretend Chromium does respond to the "continue" request immediately
    resumed(DebugEvent.CLIENT_REQUEST);
  }

  public void resumed(int detail) {
    debugContext = null;
    fireResumeEvent(detail);
  }

  public boolean canSuspend() {
    return !isDisconnected() && !isSuspended();
  }

  public void suspend() throws DebugException {
    vmEmbedder.getJavascriptVm().suspend(null);
  }

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

  public synchronized boolean isDisconnected() {
    return isDisconnected;
  }

  public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
    return null;
  }

  public boolean supportsStorageRetrieval() {
    return false;
  }

  /**
   * Fires a debug event
   *
   * @param event to be fired
   */
  public void fireEvent(DebugEvent event) {
    DebugPlugin debugPlugin = DebugPlugin.getDefault();
    if (debugPlugin != null) {
      debugPlugin.fireDebugEventSet(new DebugEvent[] { event });
    }
  }

  public void fireEventForThread(int kind, int detail) {
    try {
      IThread[] threads = getThreads();
      if (threads.length > 0) {
        fireEvent(new DebugEvent(threads[0], kind, detail));
      }
    } catch (DebugException e) {
      // Actually, this is not thrown in our getThreads()
      return;
    }
  }

  public void fireCreationEvent() {
    setDisconnected(false);
    fireEventForThread(DebugEvent.CREATE, DebugEvent.UNSPECIFIED);
  }

  public synchronized void setDisconnected(boolean disconnected) {
    isDisconnected = disconnected;
  }

  public void fireResumeEvent(int detail) {
    setSuspended(false);
    fireEventForThread(DebugEvent.RESUME, detail);
    fireEvent(new DebugEvent(this, DebugEvent.RESUME, detail));
  }

  public void fireSuspendEvent(int detail) {
    setSuspended(true);
    fireEventForThread(DebugEvent.SUSPEND, detail);
    fireEvent(new DebugEvent(this, DebugEvent.SUSPEND, detail));
  }

  public void fireTerminateEvent() {
    // TODO(peter.rybin): from Alexander Pavlov: I think you need to fire a terminate event after
    // this line, for consolePseudoProcess if one is not null.
    fireEventForThread(DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED);
    fireEvent(new DebugEvent(this, DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED));
    fireEvent(new DebugEvent(getLaunch(), DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED));
  }

  public void breakpointAdded(IBreakpoint breakpoint) {
    workspaceRelations.getBreakpointHandler().breakpointAdded(breakpoint);
  }

  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    workspaceRelations.getBreakpointHandler().breakpointChanged(breakpoint, delta);
  }

  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    workspaceRelations.getBreakpointHandler().breakpointRemoved(breakpoint, delta);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter) {
    if (adapter == EvaluateContext.class) {
      JavascriptThread thread = getThread();
      if (thread == null) {
        return null;
      }
      return thread.getAdapter(adapter);
    } else if (adapter == ILaunch.class) {
      return this.launch;
    }
    return super.getAdapter(adapter);
  }

  public VmResource getVmResource(IFile resource) throws CoreException {
    return workspaceRelations.findVmResourceFromWorkspaceFile(resource);
  }

  public JavascriptThread getThread() {
    return isDisconnected()
        ? null
        : threads[0];
  }

  private static String trim(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength - 3) + "..."; //$NON-NLS-1$
  }

  public DebugContext getDebugContext() {
    return debugContext;
  }

  private final DebugEventListenerImpl debugEventListener = new DebugEventListenerImpl();

  class DebugEventListenerImpl implements DebugEventListener, LiveEditDebugEventListener {
    // Synchronizes calls from ReaderThread of Connection and one call from some worker thread
    private final Object suspendResumeMonitor = new Object();
    private boolean alreadyResumedOrSuspended = false;

    public void disconnected() {
      if (!isDisconnected()) {
        setDisconnected(true);
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointManagerListener(
            workspaceRelations.getBreakpointHandler());
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(
            DebugTargetImpl.this);
        fireTerminateEvent();
      }
    }

    public void resumedByDefault() {
      synchronized (suspendResumeMonitor) {
        if (!alreadyResumedOrSuspended) {
          resumed();
        }
      }
    }

    public void resumed() {
      listenerBlock.waitUntilReady();
      synchronized (suspendResumeMonitor) {
        DebugTargetImpl.this.resumed(DebugEvent.CLIENT_REQUEST);
        alreadyResumedOrSuspended = true;
      }
    }

    public void scriptLoaded(Script newScript) {
      listenerBlock.waitUntilReady();
      workspaceRelations.scriptLoaded(newScript);
    }

    public void scriptCollected(Script script) {
      listenerBlock.waitUntilReady();
      workspaceRelations.scriptCollected(script);
    }

    public void scriptContentChanged(UpdatableScript newScript) {
      listenerBlock.waitUntilReady();
      workspaceRelations.reloadScript(newScript);
    }

    public void suspended(DebugContext context) {
      listenerBlock.waitUntilReady();
      synchronized (suspendResumeMonitor) {
        DebugTargetImpl.this.debugContext = context;
        workspaceRelations.getBreakpointHandler().breakpointsHit(context.getBreakpointsHit());
        int suspendedDetail;
        if (context.getState() == State.EXCEPTION) {
          logExceptionFromContext(context);
          suspendedDetail = DebugEvent.BREAKPOINT;
        } else {
          if (context.getBreakpointsHit().isEmpty()) {
            suspendedDetail = DebugEvent.STEP_END;
          } else {
            suspendedDetail = DebugEvent.BREAKPOINT;
          }
        }
        DebugTargetImpl.this.suspended(suspendedDetail);

        alreadyResumedOrSuspended = true;
      }
    }

    public VmStatusListener getVmStatusListener() {
      return vmStatusListener;
    }
  }

  private final VmStatusListenerImpl vmStatusListener = new VmStatusListenerImpl();

  private class VmStatusListenerImpl implements DebugEventListener.VmStatusListener {
    private String currentRequest = null;
    private int numberOfEnqueued;

    public synchronized void busyStatusChanged(String currentRequest, int numberOfEnqueued) {
      this.currentRequest = currentRequest;
      this.numberOfEnqueued = numberOfEnqueued;
      fireEvent(new DebugEvent(DebugTargetImpl.this, DebugEvent.CHANGE));
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


  private void logExceptionFromContext(DebugContext context) {
    ExceptionData exceptionData = context.getExceptionData();
    List<? extends CallFrame> callFrames = context.getCallFrames();
    String scriptName;
    Object lineNumber;
    if (callFrames.size() > 0) {
      CallFrame topFrame = callFrames.get(0);
      Script script = topFrame.getScript();
      scriptName = script != null ? script.getName() : Messages.DebugTargetImpl_Unknown;
      lineNumber = topFrame.getLineNumber();
    } else {
      scriptName = Messages.DebugTargetImpl_Unknown;
      lineNumber = Messages.DebugTargetImpl_Unknown;
    }
    ChromiumDebugPlugin.logError(
        Messages.DebugTargetImpl_LogExceptionFormat,
        exceptionData.isUncaught()
            ? Messages.DebugTargetImpl_Uncaught
            : Messages.DebugTargetImpl_Caught,
        exceptionData.getExceptionMessage(),
        scriptName,
        lineNumber,
        trim(exceptionData.getSourceText(), 80));
  }

  private final JavascriptVmEmbedder.Listener embedderListener =
      new JavascriptVmEmbedder.Listener() {
    public void reset() {
      listenerBlock.waitUntilReady();
      workspaceRelations.handleVmResetEvent();
      fireEvent(new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.STATE));
    }
    public void closed() {
      debugEventListener.disconnected();
    }
  };

  private final ILaunchListener launchListener = new ILaunchListener() {
    public void launchAdded(ILaunch launch) {
    }
    public void launchChanged(ILaunch launch) {
    }
    // TODO(peter.rybin): maybe have one instance of listener for all targets?
    public void launchRemoved(ILaunch launch) {
      if (launch != DebugTargetImpl.this.launch) {
        return;
      }
      DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
      workspaceRelations.launchRemoved();
    }
  };

  private final static JavascriptVmEmbedder STUB_VM_EMBEDDER = new JavascriptVmEmbedder() {
    public JavascriptVm getJavascriptVm() {
      //TODO(peter.rybin): decide and redo this exception
      throw new UnsupportedOperationException();
    }

    public String getTargetName() {
      //TODO(peter.rybin): decide and redo this exception
      throw new UnsupportedOperationException();
    }

    public String getThreadName() {
      //TODO(peter.rybin): decide and redo this exception
      throw new UnsupportedOperationException();
    }
  };

  public WorkspaceBridge.JsLabelProvider getLabelProvider() {
    return workspaceBridgeFactory.getLabelProvider();
  }

  public static List<DebugTargetImpl> getAllDebugTargetImpls() {
    IDebugTarget[] array = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
    List<DebugTargetImpl> result = new ArrayList<DebugTargetImpl>(array.length);
    for (IDebugTarget target : array) {
      if (target instanceof DebugTargetImpl == false) {
        continue;
      }
      if (target.getLaunch().isTerminated()) {
        continue;
      }
      DebugTargetImpl debugTargetImpl = (DebugTargetImpl) target;
      result.add(debugTargetImpl);
    }
    return result;
  }

  private static class ListenerBlock {
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
}
