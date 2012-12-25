// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Map;

import org.chromium.sdk.util.MethodIsBlockingException;
import org.chromium.sdk.wip.EvaluateToMappingExtension;

/**
 * A context in which watch expressions may be evaluated. Typically corresponds to stack frame
 * of suspended process, but may also be detached from any stack frame.
 *
 * @see EvaluateToMappingExtension
 */
public interface JsEvaluateContext {

  /**
   * A callback for the "evaluate" request.
   * TODO: handle compile exception separately.
   */
  interface EvaluateCallback {
    void success(ResultOrException result);

    void failure(Exception cause);
  }

  interface ResultOrException {
    JsValue getResult();
    JsValue getException();

    <R> R accept(Visitor<R> visitor);
    interface Visitor<R> {
      R visitResult(JsValue value);
      R visitException(JsValue exception);
    }
  }

  /**
   * Synchronously evaluates an arbitrary JavaScript {@code expression} in
   * the particular context.
   * Previously loaded {@link JsObject}s can be addressed from the expression if listed in
   * additionalContext parameter.
   * The evaluation result is reported to the specified {@code evaluateCallback}.
   * The method will block until the evaluation result is available.
   *
   * @param expression to evaluate
   * @param additionalContext a name-to-value map that adds new values to an expression
   *     scope; may be null
   * @param evaluateCallback to report the evaluation result to
   * @throws MethodIsBlockingException if called from a callback because it blocks
   *         until remote VM returns result
   */
  void evaluateSync(String expression, Map<String, ? extends JsValue> additionalContext,
      EvaluateCallback evaluateCallback) throws MethodIsBlockingException;

  /**
   * Asynchronously evaluates an arbitrary JavaScript {@code expression} in
   * the particular context.
   * Previously loaded {@link JsObject}s can be addressed from the expression if listed in
   * additionalContext parameter.
   * The evaluation result is reported to the specified {@code evaluateCallback}.
   * The method doesn't block.
   *
   * @param expression to evaluate
   * @param additionalContext a name-to-value map that adds new values to an expression
   *     scope; may be null
   * @param evaluateCallback to report the evaluation result to
   * @param syncCallback to report the end of any processing
   */
  RelayOk evaluateAsync(String expression, Map<String, ? extends JsValue> additionalContext,
      EvaluateCallback evaluateCallback, SyncCallback syncCallback);

  /**
   * @return factory that locally creates {@link JsValue} instances for primitive values.
   */
  PrimitiveValueFactory getValueFactory();

  /**
   * Locally creates primitive values. They can be used is such methods
   * as {@link JsEvaluateContext#evaluateAsync}
   */
  interface PrimitiveValueFactory {
    JsValue createString(String value);
    JsValue createBoolean(boolean value);
    JsValue createNumber(String stringRepresentation);
    JsValue createNumber(long value);
    JsValue createNumber(double value);
    JsValue getUndefined();
    JsValue getNull();
  }
}
