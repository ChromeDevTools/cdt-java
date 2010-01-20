// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.chromium.sdk.DebugContext.State;
import org.chromium.sdk.DebugContext.StepAction;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

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

  private final ListenerBlock listenerBlock = new ListenerBlock();

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
    listenerBlock.unblock();

    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
    reloadScriptsAndPossiblyResume(attachCallback);

    return true;
  }

  private void reloadScriptsAndPossiblyResume(final Runnable attachCallback) {
    workspaceRelations.reloadScriptsAtStart();

    try {
      if (attachCallback != null) {
        attachCallback.run();
      }
    } finally {
      fireCreationEvent();
    }

    Job job = new Job("Update debugger state") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        debugEventListener.resumedByDefault();
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  public String getName() throws DebugException {
    return workspaceBridgeFactory.getLabelProvider().getTargetLabel(this);
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
    if (ILaunch.class.equals(adapter)) {
      return this.launch;
    }
    return super.getAdapter(adapter);
  }

  public IFile getScriptResource(Script script) {
    return workspaceRelations.getScriptResource(script);
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

  class DebugEventListenerImpl implements DebugEventListener {
    // Synchronizes calls from ReaderThread of Connection and one call from some worker thread
    private final Object suspendResumeMonitor = new Object();
    private boolean alreadyResumedOrSuspended = false;

    public void disconnected() {
      if (!isDisconnected()) {
        setDisconnected(true);
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
  }

  private void logExceptionFromContext(DebugContext context) {
    ExceptionData exceptionData = context.getExceptionData();
    CallFrame topFrame = context.getCallFrames().get(0);
    Script script = topFrame.getScript();
    ChromiumDebugPlugin.logError(
        Messages.DebugTargetImpl_LogExceptionFormat,
        exceptionData.isUncaught()
            ? Messages.DebugTargetImpl_Uncaught
            : Messages.DebugTargetImpl_Caught,
        exceptionData.getExceptionMessage(),
        script != null ? script.getName() : "<unknown>", //$NON-NLS-1$
        topFrame.getLineNumber(),
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

  private static class ListenerBlock {
    private volatile boolean isBlocked = true;
    private final Object monitor = new Object();
    void waitUntilReady() {
      if (isBlocked) {
        return;
      }
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
    void unblock() {
      isBlocked = true;
      synchronized (monitor) {
        monitor.notifyAll();
      }
    }
  }
}
