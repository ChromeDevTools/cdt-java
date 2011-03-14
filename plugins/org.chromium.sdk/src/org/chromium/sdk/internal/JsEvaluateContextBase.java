// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Map;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

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
  public void evaluateAsync(final String expression, final EvaluateCallback callback,
      SyncCallback syncCallback) {
    evaluateAsync(expression, null, callback, syncCallback);
  }

  public void evaluateSync(String expression, Map<String, String> additionalContext,
      EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    evaluateAsync(expression, additionalContext, evaluateCallback, callbackSemaphore);
    boolean res = callbackSemaphore.tryAcquireDefault();
    if (!res) {
      evaluateCallback.failure("Timeout");
    }
  }

  public abstract void evaluateAsync(final String expression, Map<String, String> additionalContext,
      final EvaluateCallback callback, SyncCallback syncCallback);
}
