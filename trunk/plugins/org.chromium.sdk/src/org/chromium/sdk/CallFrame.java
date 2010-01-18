// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;
import java.util.List;

import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;


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
   * @return the variables known in this frame, including the receiver variable
   * @deprecated in favor of {@link #getVariableScopes()}
   */
  @Deprecated
  Collection<? extends JsVariable> getVariables();

  /**
   * @return the scopes known in this frame; ordered, innermost first, global scope last
   */
  List<? extends JsScope> getVariableScopes();

  /**
   * @return the receiver variable known in this frame
   */
  JsVariable getReceiverVariable();

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
   * Synchronously evaluates an arbitrary JavaScript {@code expression} in
   * the context of the call frame. The evaluation result is reported to
   * the specified {@code evaluateCallback}. The method will block until the evaluation
   * result is available.
   *
   * @param expression to evaluate
   * @param evaluateCallback to report the evaluation result to
   * @throws MethodIsBlockingException if called from a callback because it blocks
   *         until remote VM returns result
   */
  void evaluateSync(String expression, EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException;

  /**
   * Asynchronously evaluates an arbitrary JavaScript {@code expression} in
   * the context of the call frame. The evaluation result is reported to
   * the specified {@code evaluateCallback} and right after this to syncCallback.
   * The method doesn't block.
   *
   * @param expression to evaluate
   * @param evaluateCallback to report the evaluation result to
   * @param syncCallback to report the end of any processing
   */
  void evaluateAsync(String expression, EvaluateCallback evaluateCallback,
      SyncCallback syncCallback);
}
