// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.JsEvaluateContextBase;
import org.chromium.sdk.internal.wip.WipValueBuilder.JsValueBase;
import org.chromium.sdk.internal.wip.WipValueBuilder.SerializableValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.internal.wip.protocol.output.runtime.CallArgumentParam;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.MethodIsBlockingException;
import org.chromium.sdk.wip.EvaluateToMappingExtension;
import org.json.simple.JSONValue;

/**
 * Basic implementation of the abstract {@link JsEvaluateContextBase}. Class leaves unimplemented
 * parts that deal with a particular context details (callframe or global) and particular protocol
 * message.
 * @param <DATA> type of protocol message response
 */
abstract class WipEvaluateContextBase<DATA> extends JsEvaluateContextBase {

  private final WipValueLoader valueLoader;

  WipEvaluateContextBase(WipValueLoader valueLoader) {
    this.valueLoader = valueLoader;
  }

  @Override
  public RelayOk evaluateAsync(final String expression,
      Map<String, ? extends JsValue> additionalContext, EvaluateCallback callback,
      SyncCallback syncCallback) {

    return evaluateAsync(expression, additionalContext, valueLoader,
        callback, syncCallback);
  }

  @Override
  public PrimitiveValueFactory getValueFactory() {
    return PRIMITIVE_VALUE_FACTORY;
  }

  private RelayOk evaluateAsync(String expression,
      Map<String, ? extends JsValue> additionalContext, WipValueLoader destinationValueLoaderParam,
      final EvaluateCallback callback, SyncCallback syncCallback) {
    Map<String, JsValueBase> internalAdditionalContext;
    if (additionalContext == null) {
      internalAdditionalContext = null;
    } else {
      internalAdditionalContext = new LinkedHashMap<String, JsValueBase>(additionalContext.size());
      for (Map.Entry<String, ? extends JsValue> en : additionalContext.entrySet()) {
        JsValueBase jsValueBase = JsValueBase.cast(en.getValue());
        internalAdditionalContext.put(en.getKey(), jsValueBase);
      }
    }
    return evaluateAsyncImpl(expression, internalAdditionalContext,
        destinationValueLoaderParam, callback, syncCallback);
  }

  RelayOk evaluateAsyncImpl(String expression,
      Map<String, ? extends SerializableValue> additionalContext,
      WipValueLoader destinationValueLoaderParam,
      final EvaluateCallback callback, SyncCallback syncCallback) {
    if (destinationValueLoaderParam == null) {
      destinationValueLoaderParam = valueLoader;
    }
    final WipValueLoader destinationValueLoader = destinationValueLoaderParam;
    if (additionalContext != null && !additionalContext.isEmpty()) {
      WipContextBuilder contextBuilder = valueLoader.getTabImpl().getContextBuilder();
      EvaluateHack evaluateHack = contextBuilder.getEvaluateHack();
      return evaluateHack.evaluateAsync(expression, additionalContext,
          destinationValueLoader, evaluateHackHelper, callback, syncCallback);
    }

    WipParamsWithResponse<DATA> params = createRequestParams(expression, destinationValueLoader);

    GenericCallback<DATA> commandCallback;
    if (callback == null) {
      commandCallback = null;
    } else {
      commandCallback = new GenericCallback<DATA>() {
        @Override
        public void success(DATA data) {
          ResultOrException valueOrException = processResponse(data, destinationValueLoader);
          callback.success(valueOrException);
        }
        @Override
        public void failure(Exception exception) {
          callback.failure(exception.getMessage());
        }
      };
    }
    WipCommandProcessor commandProcessor = valueLoader.getTabImpl().getCommandProcessor();
    return commandProcessor.send(params, commandCallback, syncCallback);
  }

  private ResultOrException processResponse(DATA data, WipValueLoader destinationValueLoader) {
    RemoteObjectValue valueData = getRemoteObjectValue(data);

    WipValueBuilder valueBuilder = destinationValueLoader.getValueBuilder();

    final JsValue jsValue = valueBuilder.wrap(valueData);

    if (getWasThrown(data) == Boolean.TRUE) {
      return new ResultOrException() {
        @Override public JsValue getResult() {
          return null;
        }
        @Override public JsValue getException() {
          return jsValue;
        }
        @Override public <R> R accept(Visitor<R> visitor) {
          return visitor.visitException(jsValue);
        }
      };
    } else {
      return new ResultOrException() {
        @Override public JsValue getResult() {
          return jsValue;
        }
        @Override public JsValue getException() {
          return null;
        }
        @Override public <R> R accept(Visitor<R> visitor) {
          return visitor.visitResult(jsValue);
        }
      };
    }
  }

  private final EvaluateHack.EvaluateCommandHandler<DATA> evaluateHackHelper =
      new EvaluateHack.EvaluateCommandHandler<DATA>() {
    @Override
    public WipParamsWithResponse<DATA> createRequest(
        String patchedUserExpression, WipValueLoader destinationValueLoader) {
      return createRequestParams(patchedUserExpression, destinationValueLoader);
    }

    @Override
    public ResultOrException processResult(DATA response, WipValueLoader destinationValueLoader) {
      return processResponse(response, destinationValueLoader);
    }

    @Override
    public Exception processFailure(Exception cause) {
      return cause;
    }
  };

  protected abstract WipParamsWithResponse<DATA> createRequestParams(String expression,
      WipValueLoader destinationValueLoader);

  protected abstract RemoteObjectValue getRemoteObjectValue(DATA data);

  protected abstract Boolean getWasThrown(DATA data);

  static WipEvaluateContextBase<?> castArgument(JsEvaluateContext context) {
    try {
      return (WipEvaluateContextBase<?>) context;
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Incorrect evaluate context argument", e);
    }
  }

  static final EvaluateToMappingExtension EVALUATE_TO_MAPPING_EXTENSION =
      new EvaluateToMappingExtension() {
    @Override
    public void evaluateSync(JsEvaluateContext evaluateContext,
        String expression, Map<String, ? extends JsValue> additionalContext,
        RemoteValueMapping targetMapping, EvaluateCallback evaluateCallback)
        throws MethodIsBlockingException {
      CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
      RelayOk relayOk = evaluateAsync(evaluateContext, expression, additionalContext,
          targetMapping, evaluateCallback, callbackSemaphore);
      callbackSemaphore.acquireDefault(relayOk);
    }

    @Override
    public RelayOk evaluateAsync(JsEvaluateContext evaluateContext,
        String expression, Map<String, ? extends JsValue> additionalContext,
        RemoteValueMapping targetMapping, EvaluateCallback evaluateCallback,
        SyncCallback syncCallback) {
      WipEvaluateContextBase<?> contextImpl =
          WipEvaluateContextBase.castArgument(evaluateContext);
      return contextImpl.evaluateAsync(expression, additionalContext,
          evaluateCallback, syncCallback);
    }
  };

  private static final PrimitiveValueFactory PRIMITIVE_VALUE_FACTORY =
      new PrimitiveValueFactory() {
    @Override
    public JsValueBase getUndefined() {
      return new JsValueBaseImpl(JsValue.Type.TYPE_UNDEFINED) {
        @Override public String getValueString() {
          return "undefined";
        }
        @Override public CallArgumentParam createCallArgumentParam() {
          return new CallArgumentParam(false, null, null);
        }
      };
    }

    @Override
    public JsValueBase getNull() {
      return new JsValueBaseImpl(JsValue.Type.TYPE_NULL) {
        @Override public String getValueString() {
          return "null";
        }
        @Override public CallArgumentParam createCallArgumentParam() {
          return new CallArgumentParam(true, null, null);
        }
      };
    }

    @Override
    public JsValueBase createString(String value) {
      return new SimpleValue(JsValue.Type.TYPE_STRING, value);
    }

    @Override
    public JsValueBase createNumber(double value) {
      return new SimpleValue(JsValue.Type.TYPE_NUMBER, value);
    }

    @Override
    public JsValueBase createNumber(long value) {
      return new SimpleValue(JsValue.Type.TYPE_NUMBER, value);
    }

    @Override
    public JsValueBase createNumber(final String stringRepresentation) {
      return new JsValueBaseImpl(JsValue.Type.TYPE_STRING) {
        @Override public String getValueString() {
          return stringRepresentation;
        }
        @Override public CallArgumentParam createCallArgumentParam() {
          return new CallArgumentParam(true, JSONValue.parse(stringRepresentation), null);
        }
      };
    }

    @Override
    public JsValueBase createBoolean(boolean value) {
      return new SimpleValue(JsValue.Type.TYPE_BOOLEAN, value);
    }

    abstract class JsValueBaseImpl extends JsValueBase {
      private final Type type;
      JsValueBaseImpl(Type type) {
        this.type = type;
      }
      @Override public Type getType() {
        return type;
      }
      @Override public JsObject asObject() {
        return null;
      }
      @Override public boolean isTruncated() {
        return false;
      }
      @Override public RelayOk reloadHeavyValue(ReloadBiggerCallback callback,
          SyncCallback syncCallback) {
        throw new UnsupportedOperationException();
      }
      @Override public String getRefId() {
        return null;
      }
    }

    class SimpleValue extends JsValueBaseImpl {
      private final Object value;
      SimpleValue(Type type, Object value) {
        super(type);
        this.value = value;
      }
      @Override public String getValueString() {
        return value.toString();
      }
      @Override public CallArgumentParam createCallArgumentParam() {
        return new CallArgumentParam(true, value, null);
      }
    }
  };
}