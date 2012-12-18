// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.wip.WipRelayRunner.ProcessException;
import org.chromium.sdk.internal.wip.WipRelayRunner.Step;
import org.chromium.sdk.internal.wip.WipValueBuilder.SerializableValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.CallFunctionOnData;
import org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.internal.wip.protocol.output.runtime.CallArgumentParam;
import org.chromium.sdk.internal.wip.protocol.output.runtime.CallFunctionOnParams;
import org.chromium.sdk.internal.wip.protocol.output.runtime.EvaluateParams;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * Helper class that implements evaluate with additional context and
 * destination group id operation. This implementation is a hack because it adds (injects)
 * a property to the global object and works with its properties. The normal approach is when
 * the protocol itself supports this operation. As it hopefully will.
 */
public class EvaluateHack {

  private final WipTabImpl tabImpl;
  private final AtomicInteger uniqueIdCounter = new AtomicInteger(0);
  private boolean objectInjected = false;

  public EvaluateHack(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }

  /**
   * Implements evaluate with additional context and destination group id operation.
   * The implementation modifies a global object.
   * @param destinationValueLoader value loader that corresponds to the destination group
   * @param evaluateCommandHandler provides a particular request type
   */
  public RelayOk evaluateAsync(String expression, ValueNameBuilder valueNameBuidler,
      Map<String, ? extends SerializableValue> additionalContext,
      WipValueLoader destinationValueLoader, EvaluateCommandHandler<?> evaluateCommandHandler,
      final JsEvaluateContext.EvaluateCallback callback, SyncCallback syncCallback) {

    RelaySyncCallback relaySyncCallback = new RelaySyncCallback(syncCallback);

    final EvaluateSession evaluateSession = new EvaluateSession(expression, valueNameBuidler,
        additionalContext, destinationValueLoader, evaluateCommandHandler);

    final RelaySyncCallback.Guard guard = relaySyncCallback.newGuard();

    GenericCallback<Void> postEnsureCallback = new GenericCallback<Void>() {
          @Override
          public void success(Void value) {
            RelayOk relayOk = evaluateSession.run(callback, guard.getRelay());
            guard.discharge(relayOk);
          }

          @Override
          public void failure(Exception exception) {
            if (callback != null) {
              callback.failure(exception.getMessage());
            }
          }
        };

    return ensureObjectInjected(postEnsureCallback, guard.asSyncCallback());
  }

  /**
   * Provides an actual evaluate request. It may or may not refer to a particular call frame
   * or deal with other details that are out of scope of this class.
   *
   * @param <DATA> type of request's response
   */
  public interface EvaluateCommandHandler<DATA> {
    WipParamsWithResponse<DATA> createRequest(String patchedUserExpression,
        WipValueLoader destinationValueLoader);

    JsVariable processResult(DATA response, WipValueLoader destinationValueLoader,
        ValueNameBuilder valueNameBuidler);

    /**
     * Return the same exception or wraps it with a more high-level error details.
     * @return not null
     */
    Exception processFailure(Exception cause);
  }

  synchronized void pageReloaded() {
    objectInjected = false;
  }

  /**
   * Corresponds to a one evaluate operation. Holds most of parameters. It does following:
   * <ol>
   *   <li>creates a temporary object inside the main injected object,
   *   <li>puts all values from additional context thus making it a 'with' object,
   *   <li>evaluates user expression inside the 'with' operator,
   *   <li>returns result to a user callback,
   *   <li>deletes the temporary object.
   * </ol>
   *
   * It uses {@link WipRelayRunner} as an engine.
   */
  private class EvaluateSession {
    private final String userExpression;
    private final ValueNameBuilder valueNameBuidler;
    private final Map<String, ? extends SerializableValue> additionalContext;
    private final WipValueLoader destinationValueLoader;
    private final EvaluateCommandHandler<?> evaluateCommandHandler;

    private final String dataId = "d" + uniqueIdCounter.incrementAndGet();

    EvaluateSession(String expression, ValueNameBuilder valueNameBuidler,
        Map<String, ? extends SerializableValue> additionalContext,
        WipValueLoader destinationValueLoader, EvaluateCommandHandler<?> evaluateCommandHandler) {
      this.userExpression = expression;
      this.valueNameBuidler = valueNameBuidler;
      this.additionalContext = additionalContext;
      this.destinationValueLoader = destinationValueLoader;
      this.evaluateCommandHandler = evaluateCommandHandler;
    }

    RelayOk run(final JsEvaluateContext.EvaluateCallback callback, RelaySyncCallback relay) {
      WipRelayRunner.Step<JsVariable> step = createFillDataObjectStep();

      GenericCallback<JsVariable> innerCallback;
      if (callback == null) {
        innerCallback = null;
      } else {
        innerCallback = new GenericCallback<JsVariable>() {
          @Override public void success(JsVariable value) {
            callback.success(value);
          }
          @Override public void failure(Exception exception) {
            callback.failure(exception.getMessage());
          }
        };
      }

      return WipRelayRunner.run(tabImpl.getCommandProcessor(), step,
          innerCallback, relay);
    }

    /**
     * Sends request that create a temporary object and fills it with user values.
     * User values are passed as 1. 'this', 2. additional arguments to the function.
     */
    private WipRelayRunner.Step<JsVariable> createFillDataObjectStep() {
      if (additionalContext.isEmpty()) {
        throw new IllegalArgumentException("Empty context");
      }

      StringBuilder assigmentBuilder = new StringBuilder();
      StringBuilder parametersBuilder = new StringBuilder();

      String thisObjectId = null;
      final List<CallArgumentParam> additionalObjectIds = new ArrayList<CallArgumentParam>(0);
      String tempObjectRef = GLOBAL_VARIABLE_NAME + ".data." + dataId + ".";
      for (Map.Entry<String, ? extends SerializableValue> entry : additionalContext.entrySet()) {
        SerializableValue jsValueBase = entry.getValue();
        String commandParamName;
        if (thisObjectId == null && jsValueBase.getRefId() != null) {
          commandParamName = "this";
          thisObjectId = jsValueBase.getRefId();
        } else {
          commandParamName = "p" + additionalObjectIds.size();
          CallArgumentParam callArgumentParam = jsValueBase.createCallArgumentParam();
          if (callArgumentParam == null) {
            throw new IllegalArgumentException("Cannot serialize additional context property " +
                entry.getKey());
          }
          additionalObjectIds.add(callArgumentParam);
          if (parametersBuilder.length() != 0) {
            parametersBuilder.append(", ");
          }
          parametersBuilder.append(commandParamName);
        }
        assigmentBuilder.append(tempObjectRef + entry.getKey() + " = " + commandParamName + ";\n");
      }
      if (thisObjectId == null) {
        // TODO: remove this limitation in protocol.
        throw new IllegalArgumentException("At least one additional parameter must be an object");
      }

      final String functionText = "function(" + parametersBuilder + ") { " +
          GLOBAL_VARIABLE_NAME + ".data." + dataId + " = {};\n" +
          assigmentBuilder + "}";

      final String thisObjectIdFinal = thisObjectId;

      return new WipRelayRunner.SendStepWithResponse<CallFunctionOnData, JsVariable>() {
        @Override
        public WipParamsWithResponse<CallFunctionOnData> getParams() {
          List<CallArgumentParam> arguments;
          if (additionalObjectIds.isEmpty()) {
            arguments = null;
          } else {
            arguments = additionalObjectIds;
          }
          return new CallFunctionOnParams(thisObjectIdFinal, functionText, arguments, null, true);
        }

        @Override
        public Step<JsVariable> processResponse(CallFunctionOnData response) {
          if (response.wasThrown() == Boolean.TRUE) {
            return createHandleErrorStep(response.result());
          }
          return createEvaluateStep(evaluateCommandHandler);
        }

        @Override
        public Exception processFailure(Exception cause) {
          return cause;
        }
      };
    }

    private <EVAL_DATA> WipRelayRunner.Step<JsVariable> createEvaluateStep(
        final EvaluateCommandHandler<EVAL_DATA> commandHandler) {
      return new WipRelayRunner.SendStepWithResponse<EVAL_DATA, JsVariable>() {
        @Override
        public WipParamsWithResponse<EVAL_DATA> getParams() {
          String script = "with (" + GLOBAL_VARIABLE_NAME + ".data." + dataId +
              ") { return (" + userExpression + "); }";
          String wrappedExpression = "(function() {" + script +"})()";

          WipParamsWithResponse<EVAL_DATA> paramsWithResponse = commandHandler.createRequest(
              wrappedExpression, destinationValueLoader);

          return paramsWithResponse;
        }

        @Override
        public Step<JsVariable> processResponse(EVAL_DATA response) {
          JsVariable jsVariable =
              commandHandler.processResult(response, destinationValueLoader, valueNameBuidler);

          clearTempObjectAsync();

          return WipRelayRunner.createFinalStep(jsVariable);
        }

        @Override
        public Exception processFailure(Exception cause) {
          return commandHandler.processFailure(cause);
        }
      };
    }

    /**
     * Clears the temporary object. It is done asynchronously, outside the main relay, because
     * user shouldn't wait for its result.
     */
    private void clearTempObjectAsync() {
      String script = "delete " + GLOBAL_VARIABLE_NAME + ".data." + dataId + ";";
      String deleteDataExpression = "(function() {" + script +"})()";
      EvaluateParams evaluateParams =
          new EvaluateParams(deleteDataExpression, null, null, null, null, true);
      tabImpl.getCommandProcessor().send(evaluateParams, (WipCommandCallback) null, null);
    }

    /**
     * An alternative spin-off in the relay, that handles an exception we ran into.
     * The additional step is needed because the exception message is only available from
     * its 'message' pseudo-property (a getter).
     */
    private Step<JsVariable> createHandleErrorStep(final RemoteObjectValue remoteObjectValue) {
      return new WipRelayRunner.SendStepWithResponse<CallFunctionOnData, JsVariable>() {
        @Override
        public WipParamsWithResponse<CallFunctionOnData> getParams() {
          String functionText = "function() { return String(this.message); }";
          return new CallFunctionOnParams(remoteObjectValue.objectId(), functionText, null, null, true);
        }

        @Override
        public Step<JsVariable> processResponse(CallFunctionOnData response)
            throws ProcessException {
          throw new ProcessException("Helper script failed on remote: " +
              response.result().value());
        }

        @Override
        public Exception processFailure(Exception cause) {
          return cause;
        }
      };
    }
  }

  /**
   * Makes sure that we injected a helper object inside a global object.
   * This cannot be implemented as step in {@link WipRelayRunner}, because the method
   * is synchronized and cannot undergo required control inversion.
   */
  private synchronized RelayOk ensureObjectInjected(GenericCallback<Void> callback,
      SyncCallback syncCallback) {
    if (objectInjected) {
      callback.success(null);
      return RelaySyncCallback.finish(syncCallback);
    } else {
      objectInjected = true;
      return injectObject(callback, syncCallback);
    }
  }

  private RelayOk injectObject(final GenericCallback<Void> callback, SyncCallback syncCallback) {
    // 'data' is for temporary objects.
    // 'code' is for utility methods.
    String injectedObjectText = "{ data: {}, code: {}}";
    String expression = "(function() { " + GLOBAL_VARIABLE_NAME + " = " + injectedObjectText +
        " ; })()";

    EvaluateParams evaluateParams = new EvaluateParams(expression, null, false, null, null, true);

    GenericCallback<EvaluateData> wrappedCallback = new GenericCallback<EvaluateData>() {
      @Override
      public void success(EvaluateData value) {
        // TODO: check result.
        callback.success(null);
      }

      @Override
      public void failure(Exception exception) {
        callback.failure(new Exception("Failed to inject evaluate helper script into remote VM",
            exception));
      }
    };

    return tabImpl.getCommandProcessor().send(evaluateParams, wrappedCallback, syncCallback);
  }

  private static final String GLOBAL_VARIABLE_NAME = "_com_chromium_debug_helper";
}
