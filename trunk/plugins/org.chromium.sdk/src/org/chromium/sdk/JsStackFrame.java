// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import org.chromium.sdk.DebugContext.EvaluateCallback;

/**
 * An object that represents a browser JavaScript VM stack frame.
 */
public interface JsStackFrame {

  /**
   * @return the variables known in this frame, an empty array if none
   */
  JsVariable[] getVariables();

  /**
   * @return the current line number in the Script corresponding to this frame
   *         (0-based) or {@code -1} if unknown
   */
  int getLineNumber();

  /**
   * @return the start character position in the line corresponding to the
   *         current statement of this frame or {@code -1} if unknown
   */
  int getCharStart();

  /**
   * @return the end character position in the line corresponding to the current
   *         statement of this frame or {@code -1} if unknown
   */
  int getCharEnd();

  /**
   * @return the source script this stack frame is associated with
   */
  Script getScript();

  /**
   * @return the name of the current function of this frame
   */
  String getFunctionName();

  /**
   * Evaluates an arbitrary JavaScript {@code expression} in the context of the
   * stack frame. The evaluation result is reported to the specified {@code
   * callback}. The method will block until the evaluation result is available
   * if {@code isSync == true}.
   *
   * @param expression to evaluate
   * @param isSync whether to perform the request synchronously
   * @param evaluateCallback to report the evaluation result to
   */
  void evaluate(String expression, boolean isSync, EvaluateCallback evaluateCallback);

}
