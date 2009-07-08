// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;

/**
 * An object that matches the execution state of the browser JavaScript VM while
 * suspended. It reconstructs and provides access to the current state of the
 * JavaScript VM.
 */
public interface DebugContext {

  /**
   * Known JavaScript debugger step actions.
   */
  public enum StepAction {
    IN,
    NEXT,
    OUT, ;
  }

  /**
   * The suspension state.
   */
  public enum State {
    /**
     * A normal suspension (a step end or a breakpoint).
     */
    NORMAL,

    /**
     * A suspension due to an exception.
     */
    EXCEPTION, ;
  }

  /**
   * A callback for the "continue" request.
   */
  interface ContinueCallback {
    void success();

    void failure(String errorMessage);
  }

  /**
   * A callback for the "evaluate" request.
   */
  interface EvaluateCallback {
    void success(JsVariable variable);

    void failure(String errorMessage);
  }

  /**
   * @return the JavaScript VM suspension state
   */
  State getState();

  /**
   * @return the current exception state, or {@code null} if current state is
   *         not {@code EXCEPTION}
   * @see #getState()
   */
  ExceptionData getExceptionData();

  /**
   * @return current set of stack frames associated with their scripts. Clients
   *         must not modify the returned array
   */
  JsStackFrame[] getStackFrames();

  /**
   * @return a set of the breakpoints hit on VM suspension with which this
   *         context is associated. An empty collection if the suspension was
   *         not related to hitting breakpoints (e.g. a step end)
   */
  Collection<Breakpoint> getBreakpointsHit();

  /**
   * Resumes the JavaScript VM execution using a "continue" request.
   *
   * @param stepAction to perform ({@code null} means "let the VM go")
   * @param stepCount steps to perform (not used if {@code stepAction == null})
   * @param callback to invoke when the request result is ready
   */
  void continueVm(StepAction stepAction, int stepCount, ContinueCallback callback);

}
