// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.v8.StepAction;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.osgi.util.NLS;

/**
 * Represents the only Chromium V8 VM thread.
 */
public class JsThread extends DebugElementImpl
    implements IThread, IAdaptable {

  private static final IStackFrame[] EMPTY_FRAMES = new IStackFrame[0];

  /**
   * Breakpoints this thread is suspended at or <code>null</code> if none.
   */
  private IBreakpoint[] breakpoints;

  /**
   * Whether this thread is stepping. V8 does not provide information if the
   * thread is actually stepping or it is running past the last statement.
   */
  private boolean isStepping = false;

  /**
   * Constructs a new thread for the given target
   *
   * @param target
   *          VM
   */
  public JsThread(V8DebuggerToolHandler handler) {
    super(handler);
  }

  public IStackFrame[] getStackFrames() throws DebugException {
    if (isSuspended()) {
      return getHandler().getStackFrames();
    } else {
      return EMPTY_FRAMES;
    }
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
    String url = getHandler().getExecution().getUrlName();
    return NLS.bind(Messages.JsThread_ThreadLabelFormat,
        (isSuspended() ? Messages.JsThread_ThreadLabelSuspended
            : Messages.JsThread_ThreadLabelRunning),
        (url.length() > 0 ? (" : " + url) : "")); //$NON-NLS-1$ //$NON-NLS-2$
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
    return !isTerminated() && !isSuspended();
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
    try {
      setStepping(true);
      getDebugTarget().fireResumeEvent(detail);
      // The suspend event should be fired once the frames are ready
      // (in BacktraceFrameProcessor).
      getHandler().step(stepAction, null);
    } catch (IOException e) {
      throw newDebugException(
          "Failed to perform Step: " + stepAction, e); //$NON-NLS-1$
    }
  }

  public void stepInto() throws DebugException {
    step(StepAction.IN, DebugEvent.STEP_INTO);
  }

  public void stepOver() throws DebugException {
    step(StepAction.NEXT, DebugEvent.STEP_OVER);
  }

  public void stepReturn() throws DebugException {
    step(StepAction.OUT, DebugEvent.STEP_RETURN);
  }

  public boolean canTerminate() {
    return !isTerminated();
  }

  public boolean isTerminated() {
    return getDebugTarget().isDisconnected();
  }

  public void terminate() throws DebugException {
    getDebugTarget().disconnect();
  }

  /**
   * Sets whether this thread is stepping.
   */
  protected void setStepping(boolean stepping) {
    isStepping = stepping;
  }

  private DebugException newDebugException(String message, Throwable t) {
    return new DebugException(new Status(Status.ERROR,
        ChromiumDebugPlugin.PLUGIN_ID, message, t));
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
