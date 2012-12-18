// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.JsEvaluateContextBase;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.v8native.protocol.input.FailedCommandResponse.ErrorDetails;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessageFactory;
import org.chromium.sdk.internal.v8native.protocol.output.EvaluateMessage;
import org.chromium.sdk.internal.v8native.value.JsValueBase;
import org.chromium.sdk.internal.v8native.value.JsVariableImpl;
import org.chromium.sdk.internal.v8native.value.ValueMirror;
import org.chromium.sdk.util.RelaySyncCallback;
import org.json.simple.JSONValue;

/**
 * Generic implementation of {@link JsEvaluateContext}. The abstract class leaves unspecified
 * stack frame identifier (possibly null) and reference to {@link InternalContext}.
 */
abstract class JsEvaluateContextImpl extends JsEvaluateContextBase {
  public RelayOk evaluateAsyncImpl(final String expression,
      Map<String, ? extends JsValue> additionalContext,
      final EvaluateCallback callback, SyncCallback syncCallback)
      throws ContextDismissedCheckedException {

    Integer frameIdentifier = getFrameIdentifier();
    Boolean isGlobal = frameIdentifier == null ? Boolean.TRUE : null;

    List<Map.Entry<String, EvaluateMessage.Value>> internalAdditionalContext =
        convertAdditionalContextList(additionalContext, getInternalContext());

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
          public void failure(String message, ErrorDetails errorDetails) {
            callback.failure(message);
          }
        };

    return getInternalContext().sendV8CommandAsync(message, true, commandCallback,
        syncCallback);
  }

  @Override
  public RelayOk evaluateAsync(final String expression,
      Map<String, ? extends JsValue> additionalContext,
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

  @Override public PrimitiveValueFactory getValueFactory() {
    return PRIMITIVE_VALUE_FACTORY;
  }

  private void maybeRethrowContextException(ContextDismissedCheckedException ex) {
    getInternalContext().getDebugSession().maybeRethrowContextException(ex);
  }


  private static List<Map.Entry<String, EvaluateMessage.Value>> convertAdditionalContextList(
      Map<String, ? extends JsValue> source, InternalContext internalContext) {
    if (source == null) {
      return null;
    }
    final List<Map.Entry<String, EvaluateMessage.Value>> dataList =
        new ArrayList<Map.Entry<String,EvaluateMessage.Value>>(source.size());
    for (final Map.Entry<String, ? extends JsValue> en : source.entrySet()) {
      JsValueBase jsValueBase = JsValueBase.cast(en.getValue());
      final EvaluateMessage.Value value = jsValueBase.getJsonParam(internalContext);
      Map.Entry<String, EvaluateMessage.Value> convertedEntry =
          new Map.Entry<String, EvaluateMessage.Value>() {
        @Override public String getKey() {
          return en.getKey();
        }
        @Override public EvaluateMessage.Value getValue() {
          return value;
        }
        @Override public EvaluateMessage.Value setValue(EvaluateMessage.Value value) {
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

  private static final PrimitiveValueFactory PRIMITIVE_VALUE_FACTORY =
      new PrimitiveValueFactory() {
    @Override public JsValue getUndefined() {
      return new JsValueBase.Impl(JsValue.Type.TYPE_UNDEFINED, null);
    }
    @Override public JsValue getNull() {
      return new JsValueBase.Impl(JsValue.Type.TYPE_NULL, null);
    }
    @Override public JsValue createString(String value) {
      return new JsValueBase.Impl(JsValue.Type.TYPE_STRING, value);
    }
    @Override public JsValue createNumber(double value) {
      return new JsValueBase.Impl(JsValue.Type.TYPE_NUMBER, JSONValue.toJSONString(value));
    }
    @Override public JsValue createNumber(long value) {
      return new JsValueBase.Impl(JsValue.Type.TYPE_NUMBER, JSONValue.toJSONString(value));
    }
    @Override public JsValue createNumber(String stringRepresentation) {
      return new JsValueBase.Impl(JsValue.Type.TYPE_NUMBER, stringRepresentation);
    }
    @Override public JsValue createBoolean(boolean value) {
      return new JsValueBase.Impl(JsValue.Type.TYPE_BOOLEAN, JSONValue.toJSONString(value));
    }
  };
}
