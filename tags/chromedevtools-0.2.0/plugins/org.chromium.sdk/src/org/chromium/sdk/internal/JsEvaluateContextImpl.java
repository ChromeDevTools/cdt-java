// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.protocol.data.ValueHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.LoadableString.Factory;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;
import org.chromium.sdk.internal.tools.v8.V8CommandCallbackBase;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;

/**
 * Generic implementation of {@link JsEvaluateContext}. The abstract class leaves unspecified
 * stack frame identifier (possibly null) and reference to {@link InternalContext}.
 */
abstract class JsEvaluateContextImpl implements JsEvaluateContext {
  public void evaluateSync(String expression, EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    evaluateAsync(expression, evaluateCallback, callbackSemaphore);
    boolean res = callbackSemaphore.tryAcquireDefault();
    if (!res) {
      evaluateCallback.failure("Timeout");
    }
  }

  public void evaluateAsync(final String expression, final EvaluateCallback callback,
      SyncCallback syncCallback) {
    try {
      evaluateAsyncImpl(expression, callback, syncCallback);
    } catch (ContextDismissedCheckedException e) {
      getInternalContext().getDebugSession().maybeRethrowContextException(e);
      // or
      try {
        callback.failure(e.getMessage());
      } finally {
        syncCallback.callbackDone(null);
      }
    }
  }

  public void evaluateAsyncImpl(final String expression, final EvaluateCallback callback,
      SyncCallback syncCallback) throws ContextDismissedCheckedException {

    Integer frameIdentifier = getFrameIdentifier();
    Boolean isGlobal = frameIdentifier == null ? Boolean.TRUE : null;
    DebuggerMessage message =
        DebuggerMessageFactory.evaluate(expression, frameIdentifier, isGlobal, Boolean.TRUE);

    V8CommandProcessor.V8HandlerCallback commandCallback = callback == null
        ? null
        : new V8CommandCallbackBase() {
          @Override
          public void success(SuccessCommandResponse successResponse) {
            ValueHandle body;
            try {
              body = successResponse.getBody().asEvaluateBody();
            } catch (JsonProtocolParseException e) {
              throw new RuntimeException(e);
            }
            InternalContext internalContext = getInternalContext();
            Factory stringFactory = internalContext.getValueLoader().getLoadableStringFactory();
            ValueMirror mirror =
                V8Helper.createMirrorFromLookup(body, stringFactory).getValueMirror();
            JsVariable variable = new JsVariableImpl(internalContext, mirror, expression);
            callback.success(variable);
          }
          @Override
          public void failure(String message) {
            callback.failure(message);
          }
        };

    getInternalContext().sendV8CommandAsync(message, true, commandCallback,
        syncCallback);
  }

  /**
   * @return frame identifier or null if the context is not frame-related
   */
  protected abstract Integer getFrameIdentifier();

  protected abstract InternalContext getInternalContext();
}
