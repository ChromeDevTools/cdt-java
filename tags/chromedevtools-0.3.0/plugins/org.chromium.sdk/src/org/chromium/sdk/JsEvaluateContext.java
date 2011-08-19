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
   */
  interface EvaluateCallback {
    void success(JsVariable variable);

    void failure(String errorMessage);
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
   * @param additionalContext a name-to-object-ref-id map that adds new values to an expression
   *     scope; may be null
   * @param evaluateCallback to report the evaluation result to
   * @throws MethodIsBlockingException if called from a callback because it blocks
   *         until remote VM returns result
   */
  void evaluateSync(String expression, Map<String, String> additionalContext,
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
   * @param additionalContext a name-to-object-ref-id map that adds new values to an expression
   *     scope; may be null
   * @param evaluateCallback to report the evaluation result to
   * @param syncCallback to report the end of any processing
   */
  RelayOk evaluateAsync(String expression, Map<String, String> additionalContext,
      EvaluateCallback evaluateCallback, SyncCallback syncCallback);
}
