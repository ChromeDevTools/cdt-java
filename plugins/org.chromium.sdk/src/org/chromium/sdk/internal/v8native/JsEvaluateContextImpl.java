// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsValue;
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
import org.chromium.sdk.internal.v8native.value.JsVariableBase;
import org.chromium.sdk.internal.v8native.value.ValueMirror;
import org.json.simple.JSONValue;

/**
 * Generic implementation of {@link JsEvaluateContext}. The abstract class leaves unspecified
 * stack frame identifier (possibly null) and reference to {@link InternalContext}.
 */
public abstract class JsEvaluateContextImpl extends JsEvaluateContextBase {
  public RelayOk evaluateAsyncImpl(final String expression,
      Map<String, ? extends JsValue> additionalContext,
      final EvaluateCallback callback, SyncCallback syncCallback)
      throws ContextDismissedCheckedException {
    List<Map.Entry<String, EvaluateMessage.Value>> internalAdditionalContext =
        convertAdditionalContextList(additionalContext, getInternalContext());

    CallbackInternal callbackInternal;
    if (callback == null) {
      callbackInternal = null;
    } else {
      callbackInternal = new CallbackInternal() {
        @Override
        public void success(final JsValueBase value) {
          ResultOrException result = new ResultOrException() {
            @Override public JsValue getResult() {
              return value;
            }
            @Override public JsValue getException() {
              return null;
            }
            @Override public <R> R accept(Visitor<R> visitor) {
              return visitor.visitResult(value);
            }
          };
          callback.success(result);
        }

        @Override
        public void exception(final JsValueBase exception) {
          ResultOrException result = new ResultOrException() {
            @Override public JsValue getResult() {
              return null;
            }
            @Override public JsValue getException() {
              return exception;
            }
            @Override public <R> R accept(Visitor<R> visitor) {
              return visitor.visitException(exception);
            }
          };
          callback.success(result);
        }

        @Override public void failure(Exception cause) {
          callback.failure(cause);
        }
      };
    }

    return evaluateAsyncInternal(expression, internalAdditionalContext,
        callbackInternal, syncCallback);
  }

  /**
   * Internal callback for evaluate operation. It returns internal types.
   */
  public interface CallbackInternal {
    void success(JsValueBase value);
    void exception(JsValueBase exception);
    void failure(Exception cause);
  }

  public RelayOk evaluateAsyncInternal(final String expression,
      List<Map.Entry<String, EvaluateMessage.Value>> internalAdditionalContext,
      final CallbackInternal callback, SyncCallback syncCallback)
      throws ContextDismissedCheckedException {

    Integer frameIdentifier = getFrameIdentifier();
    Boolean isGlobal = frameIdentifier == null ? Boolean.TRUE : null;

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
            JsValueBase value =
                JsVariableBase.createValue(internalContext.getValueLoader(), mirror);
            callback.success(value);
          }
          @Override
          public void failure(String message, ErrorDetails errorDetails) {
            // We are not fully correct here.
            // All that we actually receive from the other side is a string message.
            // It might be message of exception or it could be diagnostic of any other
            // kind of error.
            // We incorrectly create string value out of this message and return
            // it as an exception.
            // TODO: Return actual exception value when protocol supports it.
            JsValueBase pseudoException = getValueFactory().createString(message);
            callback.exception(pseudoException);
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
      return maybeRethrowContextException(e, syncCallback);
    }
  }

  @Override public PrimitiveValueFactoryImpl getValueFactory() {
    return PrimitiveValueFactoryImpl.INSTANCE;
  }

  private RelayOk maybeRethrowContextException(ContextDismissedCheckedException ex,
      SyncCallback syncCallback) {
    return getInternalContext().getDebugSession().maybeRethrowContextException(ex, syncCallback);
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

  public static class PrimitiveValueFactoryImpl implements PrimitiveValueFactory {
    private static final PrimitiveValueFactoryImpl INSTANCE =
        new PrimitiveValueFactoryImpl();

    @Override public JsValue getUndefined() {
      return new JsValueBase.Impl(JsValue.Type.TYPE_UNDEFINED, null);
    }
    @Override public JsValue getNull() {
      return new JsValueBase.Impl(JsValue.Type.TYPE_NULL, null);
    }
    @Override public JsValueBase createString(String value) {
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
  }
}
