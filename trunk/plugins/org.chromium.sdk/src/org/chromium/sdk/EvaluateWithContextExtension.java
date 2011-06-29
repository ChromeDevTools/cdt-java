// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Map;

import org.chromium.sdk.JsEvaluateContext.EvaluateCallback;
import org.chromium.sdk.internal.v8native.MethodIsBlockingException;

/**
 * An extension to {@link JsEvaluateContext} interface, that supports additional context
 * argument. The extension is put separate because {@link JavascriptVm} may or may not
 * support it (depends on version).
 * The instance is not bound to an instance of {@link JsEvaluateContext}, instead it accepts
 * a context as a parameter.
 * <p>
 * The instance may be obtained by {@link JavascriptVm#getEvaluateWithContextExtension()}.
 */
public interface EvaluateWithContextExtension {
  /**
   * Synchronously evaluates an arbitrary JavaScript {@code expression} in
   * the context of the call frame. The evaluation result is reported to
   * the specified {@code evaluateCallback}. The method will block until the evaluation
   * result is available.
   *
   * @param evaluateContext a context in which evaluate operation works
   * @param expression to evaluate
   * @param additionalContext a map of names that will be added to an expression scope or null
   * @param evaluateCallback to report the evaluation result to
   * @throws MethodIsBlockingException if called from a callback because it blocks
   *         until remote VM returns result
   */
  void evaluateSync(JsEvaluateContext evaluateContext, String expression,
      Map<String, String> additionalContext, EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException;

  /**
   * Asynchronously evaluates an arbitrary JavaScript {@code expression} in
   * the context of the call frame. The evaluation result is reported to
   * the specified {@code evaluateCallback} and right after this to syncCallback.
   * The method doesn't block.
   *
   * @param evaluateContext a context in which evaluate operation works
   * @param expression to evaluate
   * @param additionalContext a map of names that will be added to an expression scope or null
   * @param evaluateCallback to report the evaluation result to
   * @param syncCallback to report the end of any processing
   */
  RelayOk evaluateAsync(JsEvaluateContext evaluateContext, String expression,
      Map<String, String> additionalContext, EvaluateCallback evaluateCallback,
      SyncCallback syncCallback);
}
