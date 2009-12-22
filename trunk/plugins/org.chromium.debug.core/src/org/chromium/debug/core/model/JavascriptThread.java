// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext.StepAction;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

/**
 * This class represents the only Chromium V8 VM thread.
 */
public class JavascriptThread extends DebugElementImpl implements IThread, IAdaptable {

  private static final StackFrame[] EMPTY_FRAMES = new StackFrame[0];

  /**
   * Breakpoints this thread is suspended at or <code>null</code> if none.
   */
  private IBreakpoint[] breakpoints;

  /**
   * Whether this thread is stepping. V8 does not provide information if the
   * thread is actually stepping or it is running past the last "steppable"
   * statement.
   */
  private boolean isStepping = false;

  /**
   * Cached stack
   */
  private StackFrame[] stackFrames;

  /**
   * Constructs a new thread for the given target
   *
   * @param debugTarget this thread is created for
   */
  public JavascriptThread(DebugTargetImpl debugTarget) {
    super(debugTarget);
  }

  public StackFrame[] getStackFrames() throws DebugException {
    if (isSuspended()) {
      ensureStackFrames();
      return stackFrames;
    } else {
      return EMPTY_FRAMES;
    }
  }

  public void reset() {
    this.stackFrames = null;
  }

  private void ensureStackFrames() {
    this.stackFrames = wrapStackFrames(getDebugTarget().getDebugContext().getCallFrames());
  }

  private StackFrame[] wrapStackFrames(List<? extends CallFrame> jsFrames) {
    StackFrame[] frames = new StackFrame[jsFrames.size()];
    for (int i = 0, size = frames.length; i < size; ++i) {
      frames[i] = new StackFrame(getDebugTarget(), this, jsFrames.get(i));
    }
    return frames;
  }

  public boolean hasStackFrames() throws DebugException {
    return isSuspended();
  }

  public int getPriority() throws DebugException {
    return 0;
  }

  public IStackFrame getTopStackFrame() throws DebugException {
    IStackFrame[] frames = getStackFrames();
    if (frames.length > 0) {
      return frames[0];
    }
    return null;
  }

  public String getName() throws DebugException {
    return getDebugTarget().getLabelProvider().getThreadLabel(this);
  }

  public IBreakpoint[] getBreakpoints() {
    if (breakpoints == null) {
      return new IBreakpoint[0];
    }
    return breakpoints;
  }

  protected void setBreakpoints(IBreakpoint[] breakpoints) {
    this.breakpoints = breakpoints;
  }

  public boolean canResume() {
    return isSuspended();
  }

  public boolean canSuspend() {
    return getDebugTarget().canSuspend();
  }

  public boolean isSuspended() {
    return getDebugTarget().isSuspended();
  }

  public void resume() throws DebugException {
    setStepping(false);
    getDebugTarget().resume();
  }

  public void suspend() throws DebugException {
    getDebugTarget().suspend();
  }

  public boolean canStepInto() {
    return isSuspended();
  }

  public boolean canStepOver() {
    return isSuspended();
  }

  public boolean canStepReturn() {
    return isSuspended();
  }

  public boolean isStepping() {
    return isStepping;
  }

  private void step(StepAction stepAction, int detail) throws DebugException {
    setStepping(true);
    getDebugTarget().getDebugContext().continueVm(stepAction, 1, null);
    // The suspend event should be fired once the backtrace is ready
    // (in BacktraceProcessor).
    getDebugTarget().fireResumeEvent(detail);
  }

  public void stepInto() throws DebugException {
    step(StepAction.IN, DebugEvent.STEP_INTO);
  }

  public void stepOver() throws DebugException {
    step(StepAction.OVER, DebugEvent.STEP_OVER);
  }

  public void stepReturn() throws DebugException {
    step(StepAction.OUT, DebugEvent.STEP_RETURN);
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

  /**
   * Sets whether this thread is stepping.
   */
  protected void setStepping(boolean stepping) {
    isStepping = stepping;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    if (adapter == StackFrame.class) {
      try {
        return getTopStackFrame();
      } catch (DebugException e) {
        ChromiumDebugPlugin.log(e);
      }
    }
    return super.getAdapter(adapter);
  }
}
