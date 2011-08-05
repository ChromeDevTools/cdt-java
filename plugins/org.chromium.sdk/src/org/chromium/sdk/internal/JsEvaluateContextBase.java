// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Map;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.EvaluateWithContextExtension;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.JsEvaluateContext.EvaluateCallback;
import org.chromium.sdk.internal.v8native.MethodIsBlockingException;
import org.chromium.sdk.internal.v8native.InternalContext.ContextDismissedCheckedException;

/**
 * Partial implementation of {@link JsEvaluateContext} that reduce all functionality
 * to single abstract method.
 */
public abstract class JsEvaluateContextBase implements JsEvaluateContext {
  @Override
  public void evaluateSync(String expression, EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException {
    evaluateSync(expression, null, evaluateCallback);
  }

  @Override
  public RelayOk evaluateAsync(final String expression, final EvaluateCallback callback,
      SyncCallback syncCallback) {
    return evaluateAsync(expression, null, callback, syncCallback);
  }

  public void evaluateSync(String expression, Map<String, String> additionalContext,
      EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    RelayOk relayOk =
        evaluateAsync(expression, additionalContext, evaluateCallback, callbackSemaphore);
    boolean res = callbackSemaphore.tryAcquireDefault(relayOk);
    if (!res) {
      evaluateCallback.failure("Timeout");
    }
  }

  public abstract RelayOk evaluateAsync(String expression, Map<String, String> additionalContext,
      EvaluateCallback callback, SyncCallback syncCallback);

  public static final EvaluateWithContextExtension EVALUATE_WITH_CONTEXT_EXTENSION =
      new EvaluateWithContextExtension() {
        @Override
        public void evaluateSync(JsEvaluateContext evaluateContext,
            String expression, Map<String, String> additionalContext,
            EvaluateCallback evaluateCallback) throws MethodIsBlockingException {

          JsEvaluateContextBase evaluateContextBase = (JsEvaluateContextBase) evaluateContext;
          evaluateContextBase.evaluateSync(expression, additionalContext, evaluateCallback);
        }

        @Override
        public RelayOk evaluateAsync(JsEvaluateContext evaluateContext,
            String expression, Map<String, String> additionalContext,
            EvaluateCallback evaluateCallback, SyncCallback syncCallback) {
          JsEvaluateContextBase evaluateContextBase = (JsEvaluateContextBase) evaluateContext;
          return evaluateContextBase.evaluateAsync(expression, additionalContext,
              evaluateCallback, syncCallback);
        }
      };
}
