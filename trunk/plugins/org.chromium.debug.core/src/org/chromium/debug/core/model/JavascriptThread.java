// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.toArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugContext.StepAction;
import org.chromium.sdk.ExceptionData;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

/**
 * This class represents the only Chromium V8 VM thread.
 */
public class JavascriptThread extends DebugElementImpl.WithConnected
    implements IThread, IAdaptable {

  private final RemoteEventListener remoteEventListener = new RemoteEventListener();

  private volatile StepState currentStepState = new RunningState(ResumeReason.UNSPECIFIED);
  private final Object currentStepStateMonitor = new Object();

  private volatile SuspendReason expectedSuspendReason = SuspendReason.UNSPECIFIED;

  /**
   * Holds 'suspended' state of the thread. As such has a getter to {@link DebugContext}.
   * It also keep references to basic enclosing objects.
   */
  public interface SuspendedState {
    JavascriptThread getThread();

    DebugContext getDebugContext();

    /**
     * Unsafe asynchronous getter: may return false, while the actual value has become true.
     */
    boolean isDismissed();
  }

  /**
   * Visitor that is used to describe thread state in UI. It doesn't expose too much of internals.
   */
  public interface StateVisitor<R> {
    R visitResumed(ResumeReason resumeReason);
    R visitSuspended(IBreakpoint[] breakpoints, ExceptionData exceptionData);
  }

  /**
   * Constructs a new thread for the given target
   *
   * @param connectedTargetData this thread is created for
   */
  public JavascriptThread(ConnectedTargetData connectedTargetData) {
    super(connectedTargetData);
  }

  /**
   * @return a separated interface for all remote events dispatching
   */
  RemoteEventListener getRemoteEventListener() {
    return remoteEventListener;
  }

  ISuspendResume getSuspendResumeAspect() {
    return suspendResumeAspect;
  }

  public StackFrameBase[] getStackFrames() throws DebugException {
    return currentStepState.getStackFrames();
  }

  /**
   * @return expose some information about thread state for UI presentation
   */
  public <R> R describeState(StateVisitor<R> visitor) {
    return currentStepState.describeState(visitor);
  }

  private static StackFrameBase[] wrapStackFrames(JavascriptThread.SuspendedState threadState) {
    DebugContext debugContext = threadState.getDebugContext();
    List<? extends CallFrame> jsFrames = debugContext.getCallFrames();
    List<StackFrameBase> result = new ArrayList<StackFrameBase>(jsFrames.size() + 1);

    ExceptionData exceptionData = debugContext.getExceptionData();
    if (exceptionData != null) {
      // Add fake 'throw exception' frame.
      EvaluateContext evaluateContext =
          new EvaluateContext(debugContext.getGlobalEvaluateContext(), threadState);
      result.add(new ExceptionStackFrame(evaluateContext, exceptionData));
    }
    for (CallFrame jsFrame : jsFrames) {
      result.add(new StackFrame(threadState, jsFrame));
    }
    return toArray(result, StackFrameBase.class);
  }

  /**
   * A fake stackframe that represents 'throwing exception'. It's a frame that holds an exception
   * as its only variable. This might be the only means to expose exception value to user because
   * exception may be raised with no frames on stack (e.g. compile error).
   */
  private static class ExceptionStackFrame extends StackFrameBase {
    private final IVariable[] variables;
    private final ExceptionData exceptionData;

    private ExceptionStackFrame(EvaluateContext evaluateContext, ExceptionData exceptionData) {
      super(evaluateContext);
      this.exceptionData = exceptionData;

      Variable variable = Variable.NamedHolder.forException(evaluateContext, exceptionData);
      variables = new IVariable[] { variable };
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
      return variables;
    }

    @Override
    public boolean hasVariables() throws DebugException {
      return variables.length > 0;
    }

    @Override
    public int getLineNumber() throws DebugException {
      return -1;
    }

    @Override
    public int getCharStart() throws DebugException {
      return -1;
    }

    @Override
    public int getCharEnd() throws DebugException {
      return getCharStart();
    }

    @Override
    public String getName() throws DebugException {
      return "<throwing exception>";
    }

    @Override
    Object getObjectForEquals() {
      return exceptionData;
    }

    @Override
    boolean isRegularFrame() {
      return false;
    }
  }

  public boolean hasStackFrames() throws DebugException {
    return isSuspended();
  }

  public int getPriority() throws DebugException {
    return 0;
  }

  public IStackFrame getTopStackFrame() throws DebugException {
    StackFrameBase[] frames = getStackFrames();
    if (frames.length == 0) {
      return null;
    }
    if (frames[0].isRegularFrame()) {
      return frames[0];
    }
    if (frames.length < 1) {
      return null;
    }
    return frames[1];
  }

  public String getName() throws DebugException {
    return getDebugTarget().getLabelProvider().getThreadLabel(this);
  }

  public IBreakpoint[] getBreakpoints() {
    return currentStepState.getBreakpoints();
  }

  public boolean canResume() {
    return suspendResumeAspect.canResume();
  }

  public boolean canSuspend() {
    return suspendResumeAspect.canSuspend();
  }

  public boolean isSuspended() {
    return suspendResumeAspect.isSuspended();
  }

  public void resume() throws DebugException {
    suspendResumeAspect.resume();
  }

  public void suspend() throws DebugException {
    suspendResumeAspect.suspend();
  }

  public boolean canStepInto() {
    return currentStepState.canStep();
  }

  public boolean canStepOver() {
    return currentStepState.canStep();
  }

  public boolean canStepReturn() {
    return currentStepState.canStep();
  }

  public boolean isStepping() {
    return currentStepState.isStepping();
  }

  public void stepInto() throws DebugException {
    currentStepState.step(StepAction.IN, ResumeReason.STEP_INTO);
  }

  public void stepOver() throws DebugException {
    currentStepState.step(StepAction.OVER, ResumeReason.STEP_OVER);
  }

  public void stepReturn() throws DebugException {
    currentStepState.step(StepAction.OUT, ResumeReason.STEP_RETURN);
  }

  public boolean canTerminate() {
    return getDebugTarget().canTerminate();
  }

  public boolean isTerminated() {
    return getDebugTarget().isTerminated();
  }

  public void terminate() throws DebugException {
    getDebugTarget().terminate();
  }

  EvaluateContext getEvaluateContext() {
    return currentStepState.getEvaluateContext();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    if (adapter == EvaluateContext.class) {
      return getEvaluateContext();
    }
    return super.getAdapter(adapter);
  }

  class RemoteEventListener {

    void suspended(DebugContext context) {
      SuspendedStateImpl suspendedState;
      synchronized (currentStepStateMonitor) {
        if (currentStepState.isSuspended()) {
          throw new IllegalStateException("Already in suspended state");
        }
        suspendedState = new SuspendedStateImpl(context);
        currentStepState = suspendedState;
      }

      WorkspaceBridge workspaceRelations = getConnectedData().getWorkspaceRelations();
      Collection<? extends Breakpoint> sdkBreakpointsHit = context.getBreakpointsHit();
      Collection<? extends IBreakpoint> uiBreakpointsHit =
          workspaceRelations.getBreakpointHandler().breakpointsHit(sdkBreakpointsHit);

      suspendedState.setBreakpoints(uiBreakpointsHit);

      SuspendReason suspendedReason;
      if (context.getState() == org.chromium.sdk.DebugContext.State.EXCEPTION) {
        suspendedReason = SuspendReason.BREAKPOINT;
      } else {
        if (sdkBreakpointsHit.isEmpty()) {
          suspendedReason = expectedSuspendReason;
        } else {
          suspendedReason = SuspendReason.BREAKPOINT;
        }
      }

      int suspendedDetail;
      if (suspendedReason == null) {
        suspendedDetail = DebugEvent.UNSPECIFIED;
      } else {
        suspendedDetail = suspendedReason.detailCode;
      }
      getConnectedData().fireSuspendEvent(suspendedDetail);
    }

    void resumed(ResumeReason resumeReason) {
      synchronized (currentStepStateMonitor) {
        if (!currentStepState.isSuspended()) {
          // Ignore.
          return;
        }
        currentStepState.dismiss();
        currentStepState = new RunningState(resumeReason);
      }
      if (resumeReason == null) {
        resumeReason = ResumeReason.UNSPECIFIED;
      }
      getConnectedData().fireResumeEvent(resumeReason.detailCode);
    }

  }

  private static abstract class StepState {
    abstract EvaluateContext getEvaluateContext();

    abstract IBreakpoint[] getBreakpoints();

    abstract StackFrameBase[] getStackFrames();

    abstract boolean isSuspended();
    abstract void resume();
    abstract boolean canSuspend();
    abstract void suspend();

    abstract boolean isStepping();
    abstract void step(StepAction stepAction, ResumeReason resumeReason);
    abstract boolean canStep();

    abstract <R> R describeState(StateVisitor<R> visitor);

    abstract void dismiss();
  }

  private class RunningState extends StepState {
    private final ResumeReason resumeReason;

    RunningState(ResumeReason resumeReason) {
      this.resumeReason = resumeReason;
    }

    @Override boolean isSuspended() {
      return false;
    }

    @Override boolean canSuspend() {
      return true;
    }

    @Override
    void suspend() {
      expectedSuspendReason = SuspendReason.CLIENT_REQUEST;
      getConnectedData().getJavascriptVm().suspend(null);
    }

    @Override StackFrameBase[] getStackFrames() {
      return EMPTY_FRAMES;
    }

    @Override IBreakpoint[] getBreakpoints() {
      return EMPTY_BREAKPOINTS;
    }

    @Override boolean isStepping() {
      return resumeReason.isStepping;
    }

    @Override boolean canStep() {
      return false;
    }

    @Override void step(StepAction stepAction, ResumeReason resumeReason) {
      // Ignore.
    }

    @Override void resume() {
      // Ignore.
    }

    @Override EvaluateContext getEvaluateContext() {
      return null;
    }

    @Override void dismiss() {
    }

    @Override
    <R> R describeState(StateVisitor<R> visitor) {
      return visitor.visitResumed(resumeReason);
    }
  }

  private class SuspendedStateImpl extends StepState implements SuspendedState {
    private final DebugContext context;
    private volatile boolean isDismissed = false;

    /**
     * Breakpoints this thread is suspended at or empty array if none.
     */
    private volatile IBreakpoint[] breakpoints = EMPTY_BREAKPOINTS;

    /**
     * Cached stack
     */
    private final AtomicReference<StackFrameBase[]> stackFrames =
        new AtomicReference<StackFrameBase[]>(null);

    SuspendedStateImpl(DebugContext context) {
      this.context = context;
    }

    @Override public JavascriptThread getThread() {
      return JavascriptThread.this;
    }

    @Override public DebugContext getDebugContext() {
      return context;
    }

    @Override void dismiss() {
      isDismissed = true;
    }

    @Override public boolean isDismissed() {
      return isDismissed;
    }

    void setBreakpoints(Collection<? extends IBreakpoint> uiBreakpoints) {
      this.breakpoints = toArray(uiBreakpoints, IBreakpoint.class);
    }

    @Override boolean isSuspended() {
      return true;
    }

    @Override boolean canSuspend() {
      return false;
    }

    @Override void suspend() {
      // Ignore.
    }

    @Override boolean canStep() {
      return true;
    }

    @Override
    void resume() {
      continueVm(StepAction.CONTINUE, ResumeReason.CLIENT_REQUEST, SuspendReason.UNSPECIFIED);
    }

    @Override
    void step(StepAction stepAction, ResumeReason resumeReason) {
      continueVm(stepAction, resumeReason, SuspendReason.STEP_END);
    }

    private void continueVm(StepAction stepAction, final ResumeReason resumeReason,
        SuspendReason futureSuspendReason) {
      expectedSuspendReason = futureSuspendReason;

      DebugContext.ContinueCallback callback = new DebugContext.ContinueCallback() {
        @Override public void success() {
          remoteEventListener.resumed(resumeReason);
        }

        @Override public void failure(String errorMessage) {
          ChromiumDebugPlugin.log(new Exception("Failed to resume: " + errorMessage));
        }
      };

      context.continueVm(stepAction, 1, callback);
    }

    @Override
    StackFrameBase[] getStackFrames() {
      StackFrameBase[] result = stackFrames.get();
      if (result == null) {
        result = wrapStackFrames(this);
        stackFrames.compareAndSet(null, result);
        result = stackFrames.get();
      }
      return result;
    }

    @Override IBreakpoint[] getBreakpoints() {
      return breakpoints;
    }

    @Override boolean isStepping() {
      return false;
    }

    @Override EvaluateContext getEvaluateContext() {
      return new EvaluateContext(context.getGlobalEvaluateContext(), this);
    }

    @Override
    <R> R describeState(StateVisitor<R> visitor) {
      return visitor.visitSuspended(breakpoints, context.getExceptionData());
    }
  }

  private final ISuspendResume suspendResumeAspect = new ISuspendResume() {
    @Override public boolean canResume() {
      return !isDisconnected() && isSuspended();
    }

    @Override public boolean isSuspended() {
      return !isDisconnected() && currentStepState.isSuspended();
    }

    @Override public void resume() throws DebugException {
      currentStepState.resume();
    }

    @Override public boolean canSuspend() {
      return !isDisconnected() && currentStepState.canSuspend();
    }

    @Override public void suspend() throws DebugException {
      currentStepState.suspend();
    }

    private boolean isDisconnected() {
      return getConnectedData().isDisconnected();
    }
  };

  /**
   * Wraps Eclipse mixed-up constants in a dedicated enum type.
   */
  enum ResumeReason {
    STEP_INTO(DebugEvent.STEP_INTO, true),
    STEP_OVER(DebugEvent.STEP_OVER, true),
    STEP_RETURN(DebugEvent.STEP_RETURN, true),
    CLIENT_REQUEST(DebugEvent.CLIENT_REQUEST, false),
    UNSPECIFIED(DebugEvent.UNSPECIFIED, false);

    private final int detailCode;
    private final boolean isStepping;

    ResumeReason(int detailCode, boolean isStepping) {
      this.detailCode = detailCode;
      this.isStepping = isStepping;
    }
  }

  /**
   * Wraps Eclipse mixed-up constants in a dedicated enum type.
   */
  private enum SuspendReason {
    STEP_END(DebugEvent.STEP_END),
    CLIENT_REQUEST(DebugEvent.CLIENT_REQUEST),
    BREAKPOINT(DebugEvent.BREAKPOINT),
    UNSPECIFIED(DebugEvent.UNSPECIFIED);

    final int detailCode;

    SuspendReason(int detailCode) {
      this.detailCode = detailCode;
    }
  }

  private static final StackFrame[] EMPTY_FRAMES = new StackFrame[0];
  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];
}
