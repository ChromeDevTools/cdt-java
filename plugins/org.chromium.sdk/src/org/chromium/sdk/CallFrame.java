// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;


/**
 * An object that represents a browser JavaScript VM call frame.
 */
public interface CallFrame {

  /**
   * A callback for the "evaluate" request.
   */
  interface EvaluateCallback {
    void success(JsVariable variable);

    void failure(String errorMessage);
  }

  /**
   * @return the variables known in this frame
   */
  Collection<? extends JsVariable> getVariables();

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
   * @return the source script this call frame is associated with. {@code null}
   *         if no script is associated with the call frame (e.g. an exception
   *         could have been thrown in a native script)
   */
  Script getScript();

  /**
   * @return the name of the current function of this frame
   */
  String getFunctionName();

  /**
   * Evaluates an arbitrary JavaScript {@code expression} in the context of the
   * call frame. The evaluation result is reported to the specified {@code
   * callback}. The method will block until the evaluation result is available
   * if {@code isSync == true}.
   *
   * @param expression to evaluate
   * @param isSync whether to perform the request synchronously
   * @param evaluateCallback to report the evaluation result to
   */
  void evaluate(String expression, boolean isSync, EvaluateCallback evaluateCallback);

}
