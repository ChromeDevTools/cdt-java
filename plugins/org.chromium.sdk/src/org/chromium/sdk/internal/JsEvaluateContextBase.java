// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal;

import java.util.Map;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * Partial implementation of {@link JsEvaluateContext} that reduce all functionality
 * to single abstract method.
 */
public abstract class JsEvaluateContextBase implements JsEvaluateContext {
  @Override
  public void evaluateSync(String expression, Map<String, ? extends JsValue> additionalContext,
      EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    RelayOk relayOk =
        evaluateAsync(expression, additionalContext, evaluateCallback, callbackSemaphore);
    boolean res = callbackSemaphore.tryAcquireDefault(relayOk);
    if (!res) {
      evaluateCallback.failure(new Exception("Timeout"));
    }
  }

  @Override
  public abstract RelayOk evaluateAsync(String expression,
      Map<String, ? extends JsValue> additionalContext,
      EvaluateCallback callback, SyncCallback syncCallback);
}
