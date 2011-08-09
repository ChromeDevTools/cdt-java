// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

import java.util.Map;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.JsEvaluateContext.EvaluateCallback;
import org.chromium.sdk.internal.v8native.MethodIsBlockingException;

/**
 * An extension to evaluate methods, that provides an additional argument 'targetMapping'.
 * The extension is available from
 * {@link WipJavascriptVm#getEvaluateWithDestinationMappingExtension()}.
 */
public interface EvaluateToMappingExtension {
  /**
   * Synchronously evaluates an arbitrary JavaScript {@code expression} in
   * the particular context. The evaluation result is reported to
   * the specified {@code evaluateCallback}. The method will block until the evaluation
   * result is available.
   *
   * @param evaluateContext a context in which evaluate operation works
   * @param expression to evaluate
   * @param additionalContext a map of names that will be added to an expression scope or null
   * @param targetMapping mapping the result must belong to
   * @param evaluateCallback to report the evaluation result to
   * @throws MethodIsBlockingException if called from a callback because it blocks
   *         until remote VM returns result
   */
  void evaluateSync(JsEvaluateContext evaluateContext, String expression,
      Map<String, String> additionalContext, RemoteValueMapping targetMapping,
      EvaluateCallback evaluateCallback) throws MethodIsBlockingException;

  /**
   * Asynchronously evaluates an arbitrary JavaScript {@code expression} in
   * the particular context. The evaluation result is reported to
   * the specified {@code evaluateCallback} and right after this to syncCallback.
   * The method doesn't block.
   *
   * @param evaluateContext a context in which evaluate operation works
   * @param expression to evaluate
   * @param additionalContext a map of names that will be added to an expression scope or null
   * @param targetMapping mapping the result must belong to
   * @param evaluateCallback to report the evaluation result to
   * @param syncCallback to report the end of any processing
   */
  RelayOk evaluateAsync(JsEvaluateContext evaluateContext, String expression,
      Map<String, String> additionalContext, RemoteValueMapping targetMapping,
      EvaluateCallback evaluateCallback, SyncCallback syncCallback);
}
