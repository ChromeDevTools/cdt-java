// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.JsEvaluateContextBase;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessageFactory;
import org.chromium.sdk.internal.v8native.value.JsObjectBase;
import org.chromium.sdk.internal.v8native.value.JsVariableImpl;
import org.chromium.sdk.internal.v8native.value.ValueMirror;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * Generic implementation of {@link JsEvaluateContext}. The abstract class leaves unspecified
 * stack frame identifier (possibly null) and reference to {@link InternalContext}.
 */
abstract class JsEvaluateContextImpl extends JsEvaluateContextBase {
  public RelayOk evaluateAsyncImpl(final String expression, Map<String, String> additionalContext,
      final EvaluateCallback callback, SyncCallback syncCallback)
      throws ContextDismissedCheckedException {

    Integer frameIdentifier = getFrameIdentifier();
    Boolean isGlobal = frameIdentifier == null ? Boolean.TRUE : null;

    List<Map.Entry<String, Integer>> internalAdditionalContext =
        convertAdditionalContextList(additionalContext);

    DebuggerMessage message = DebuggerMessageFactory.evaluate(expression, frameIdentifier,
        isGlobal, Boolean.TRUE, internalAdditionalContext);

    V8CommandProcessor.V8HandlerCallback commandCallback = callback == null
        ? null
        : new V8CommandCallbackBase() {
          @Override
          public void success(SuccessCommandResponse successResponse) {
            ValueHandle body;
            try {
              body = successResponse.body().asEvaluateBody();
            } catch (JsonProtocolParseException e) {
              throw new RuntimeException(e);
            }
            InternalContext internalContext = getInternalContext();
            ValueMirror mirror = internalContext.getValueLoader().addDataToMap(body);
            JsVariable variable = new JsVariableImpl(internalContext.getValueLoader(),
                mirror, expression);
            callback.success(variable);
          }
          @Override
          public void failure(String message) {
            callback.failure(message);
          }
        };

    return getInternalContext().sendV8CommandAsync(message, true, commandCallback,
        syncCallback);
  }

  @Override
  public RelayOk evaluateAsync(final String expression, Map<String, String> additionalContext,
      final EvaluateCallback callback, SyncCallback syncCallback) {
    try {
      return evaluateAsyncImpl(expression, additionalContext, callback, syncCallback);
    } catch (ContextDismissedCheckedException e) {
      maybeRethrowContextException(e);
      // or
      callback.failure(e.getMessage());
      return RelaySyncCallback.finish(syncCallback);
    }
  }

  private void maybeRethrowContextException(ContextDismissedCheckedException ex) {
    getInternalContext().getDebugSession().maybeRethrowContextException(ex);
  }


  private static List<Map.Entry<String, Integer>> convertAdditionalContextList(
      Map<String, String> source) {
    if (source == null) {
      return null;
    }
    final List<Map.Entry<String, Integer>> dataList =
        new ArrayList<Map.Entry<String,Integer>>(source.size());
    for (final Map.Entry<String, String> en : source.entrySet()) {
      final int refValue = JsObjectBase.parseRefId(en.getValue());
      Map.Entry<String, Integer> convertedEntry = new Map.Entry<String, Integer>() {
        public String getKey() {
          return en.getKey();
        }
        public Integer getValue() {
          return refValue;
        }
        public Integer setValue(Integer value) {
          throw new UnsupportedOperationException();
        }
      };
      dataList.add(convertedEntry);
    }
    return dataList;
  }

  /**
   * @return frame identifier or null if the context is not frame-related
   */
  protected abstract Integer getFrameIdentifier();

  protected abstract InternalContext getInternalContext();
}
