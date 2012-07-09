// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.Map;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.JsEvaluateContextBase;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.MethodIsBlockingException;
import org.chromium.sdk.wip.EvaluateToMappingExtension;

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
      Map<String, String> additionalContext, final EvaluateCallback callback,
      SyncCallback syncCallback) {

    WipExpressionBuilder.ValueNameBuilder valueNameBuilder =
        WipExpressionBuilder.createRootName(expression, true);

    return evaluateAsync(expression, valueNameBuilder, additionalContext, valueLoader,
        callback, syncCallback);
  }

  RelayOk evaluateAsync(String expression, ValueNameBuilder valueNameBuidler,
      Map<String, String> additionalContext, final EvaluateCallback callback,
      SyncCallback syncCallback) {
    return evaluateAsync(expression, valueNameBuidler, additionalContext, valueLoader,
        callback, syncCallback);
  }

  private RelayOk evaluateAsync(String expression, final ValueNameBuilder valueNameBuidler,
      Map<String, String> additionalContext, WipValueLoader destinationValueLoaderParam,
      final EvaluateCallback callback, SyncCallback syncCallback) {
    if (destinationValueLoaderParam == null) {
      destinationValueLoaderParam = valueLoader;
    }
    final WipValueLoader destinationValueLoader = destinationValueLoaderParam;
    if (additionalContext != null && !additionalContext.isEmpty()) {
      WipContextBuilder contextBuilder = valueLoader.getTabImpl().getContextBuilder();
      EvaluateHack evaluateHack = contextBuilder.getEvaluateHack();
      return evaluateHack.evaluateAsync(expression, valueNameBuidler, additionalContext,
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
          JsVariable variable = processResponse(data, destinationValueLoader, valueNameBuidler);
          callback.success(variable);
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

  private JsVariable processResponse(DATA data, WipValueLoader destinationValueLoader,
      ValueNameBuilder valueNameBuidler) {
    RemoteObjectValue valueData = getRemoteObjectValue(data);

    WipValueBuilder valueBuilder = destinationValueLoader.getValueBuilder();

    if (getWasThrown(data) == Boolean.TRUE) {
      return WipContextBuilder.wrapExceptionValue(valueData, valueBuilder);
    } else {
      return valueBuilder.createVariable(valueData, valueNameBuidler);
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
    public JsVariable processResult(DATA response, WipValueLoader destinationValueLoader,
        ValueNameBuilder valueNameBuidler) {
      return processResponse(response, destinationValueLoader, valueNameBuidler);
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
        String expression, Map<String, String> additionalContext,
        RemoteValueMapping targetMapping, EvaluateCallback evaluateCallback)
        throws MethodIsBlockingException {
      CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
      RelayOk relayOk = evaluateAsync(evaluateContext, expression, additionalContext,
          targetMapping, evaluateCallback, callbackSemaphore);
      callbackSemaphore.acquireDefault(relayOk);
    }

    @Override
    public RelayOk evaluateAsync(JsEvaluateContext evaluateContext,
        String expression, Map<String, String> additionalContext,
        RemoteValueMapping targetMapping, EvaluateCallback evaluateCallback,
        SyncCallback syncCallback) {
      WipEvaluateContextBase<?> contextImpl =
          WipEvaluateContextBase.castArgument(evaluateContext);
      return contextImpl.evaluateAsync(expression, additionalContext,
          evaluateCallback, syncCallback);
    }
  };
}