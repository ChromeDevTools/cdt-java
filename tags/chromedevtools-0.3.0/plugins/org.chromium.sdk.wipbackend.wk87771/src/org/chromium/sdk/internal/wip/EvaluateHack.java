// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.wip.WipRelayRunner.ProcessException;
import org.chromium.sdk.internal.wip.WipRelayRunner.Step;
import org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData;
import org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateOnData;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.internal.wip.protocol.output.runtime.EvaluateOnParams;
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
  public RelayOk evaluateAsync(String expression, Map<String, String> additionalContext,
      WipValueLoader destinationValueLoader, EvaluateCommandHandler<?> evaluateCommandHandler,
      final JsEvaluateContext.EvaluateCallback callback, SyncCallback syncCallback) {

    RelaySyncCallback relaySyncCallback = new RelaySyncCallback(syncCallback);

    final EvaluateSession evaluateSession = new EvaluateSession(expression, additionalContext,
        destinationValueLoader, evaluateCommandHandler);

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
        String originalExpression);

    /**
     * Return the same exception or wraps it with a more high-level error details.
     * @return not null
     */
    Exception processFailure(Exception cause);
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
    private final Map<String, String> additionalContext;
    private final WipValueLoader destinationValueLoader;
    private final EvaluateCommandHandler<?> evaluateCommandHandler;

    private final String dataId = "d" + uniqueIdCounter.incrementAndGet();

    EvaluateSession(String expression, Map<String, String> additionalContext,
        WipValueLoader destinationValueLoader, EvaluateCommandHandler<?> evaluateCommandHandler) {
      this.userExpression = expression;
      this.additionalContext = additionalContext;
      this.destinationValueLoader = destinationValueLoader;
      this.evaluateCommandHandler = evaluateCommandHandler;
    }

    RelayOk run(final JsEvaluateContext.EvaluateCallback callback, RelaySyncCallback relay) {
      WipRelayRunner.Step<JsVariable> step = createCreateTempObjectStep();

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

    private WipRelayRunner.Step<JsVariable> createCreateTempObjectStep() {
      String script = GLOBAL_VARIABLE_NAME + ".data." + dataId + " = {}";
      final String assignExpression = "(function() {" + script +"})()";

      return new WipRelayRunner.SendStepWithResponse<EvaluateData, JsVariable>() {
        @Override public WipParamsWithResponse<EvaluateData> getParams() {
          return new EvaluateParams(assignExpression, null, null);
        }

        @Override
        public Step<JsVariable> processResponse(EvaluateData response) throws ProcessException {
          if (response.wasThrown() == Boolean.TRUE) {
            return createHandleErrorStep(response.result());
          }
          return createFillDataObjectStep(additionalContext.entrySet().iterator());
        }

        @Override
        public Exception processFailure(Exception cause) {
          return cause;
        }
      };
    }

    private WipRelayRunner.Step<JsVariable> createFillDataObjectStep(
        final Iterator<Map.Entry<String, String>> it) {
      if (!it.hasNext()) {
        return createEvaluateStep(evaluateCommandHandler);
      }

      Map.Entry<String, String> contextEntry = it.next();

      String tempObjectRef = GLOBAL_VARIABLE_NAME + ".data." + dataId + ".";

      final String functionText = "(function(p) { " + tempObjectRef + contextEntry.getKey() +
          " = p; })(this)";

      final String thisObjectId = contextEntry.getValue();

      return new WipRelayRunner.SendStepWithResponse<EvaluateOnData, JsVariable>() {
        @Override
        public WipParamsWithResponse<EvaluateOnData> getParams() {
          return new EvaluateOnParams(thisObjectId, functionText);
        }

        @Override
        public Step<JsVariable> processResponse(EvaluateOnData response) {
          if (response.wasThrown() == Boolean.TRUE) {
            return createHandleErrorStep(response.result());
          }
          return createFillDataObjectStep(it);
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
              commandHandler.processResult(response, destinationValueLoader, userExpression);

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
          new EvaluateParams(deleteDataExpression, null, null);
      tabImpl.getCommandProcessor().send(evaluateParams, (WipCommandCallback) null, null);
    }

    /**
     * An alternative spin-off in the relay, that handles an exception we ran into.
     * The additional step is needed because the exception message is only available from
     * its 'message' pseudo-property (a getter).
     */
    private Step<JsVariable> createHandleErrorStep(final RemoteObjectValue remoteObjectValue) {
      return new WipRelayRunner.SendStepWithResponse<EvaluateOnData, JsVariable>() {
        @Override
        public WipParamsWithResponse<EvaluateOnData> getParams() {
          String functionText = "function() { return String(this.message); }";
          return new EvaluateOnParams(remoteObjectValue.objectId(), functionText);
        }

        @Override
        public Step<JsVariable> processResponse(EvaluateOnData response)
            throws ProcessException {
          throw new ProcessException("Helper script failed on remote: " +
              response.result().description());
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

    EvaluateParams evaluateParams = new EvaluateParams(expression, null, false);

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
