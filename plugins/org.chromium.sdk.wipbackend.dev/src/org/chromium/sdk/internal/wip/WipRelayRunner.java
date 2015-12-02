// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.output.WipParams;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * A utility class that helps running a chain of asynchronous commands in a safe manner.
 * 'Safe' here means that the client will get {@link SyncCallback} called in the end
 * in any scenario.
 * <p>
 * The class helps reformat control sequence: instead of being callback-driven, the program
 * becomes step-driven. Each step defines a request being sent, the way a response is processed
 * and a link to the next step.
 * <p>
 * The class is bound to {@link WipCommandProcessor}.
 */
public class WipRelayRunner {

  /**
   * The main abstraction of {@link WipRelayRunner}. A particular relay consists is a chain
   * of steps. Each step returns a next step except for the 'final' step
   * (see {@link WipRelayRunner#createFinalStep(Object)}).
   * <p>This type is essentially an algebraic data type.
   *
   * @param <RES> a type that the entire relay should return
   */
  interface Step<RES> {
    <R> R accept(Visitor<R, RES> visitor);

    interface Visitor<R, RES> {
      R visitFinal(RES finalResult);
      R visitSend(SendStepSimple<RES> sendStep);
      <DATA> R visitSend(SendStepWithResponse<DATA, RES> sendStep);
    }
  }

  /**
   * Creates a final step that simply holds a result.
   */
  public static <RES> Step<RES> createFinalStep(final RES finalResult) {
    return new Step<RES>() {
      @Override
      public <R> R accept(Visitor<R, RES> visitor) {
        return visitor.visitFinal(finalResult);
      }
    };
  }

  /**
   * A base class for 'send' step that sends a request and processes its response.
   * @param <RES> a type that the entire relay should return
   */
  public static abstract class SendStepSimple<RES> implements Step<RES> {
    public abstract WipParams getParams();

    /**
     * Handles normal response and returns a next step (or throws {@link ProcessException}).
     * The response itself contains no data, so there's no such parameter.
     */
    public abstract Step<RES> processResponse() throws ProcessException;

    /**
     * Optionally wraps the cause with a more high-level exception. Must return cause by default.
     * @return not null
     */
    public abstract Exception processFailure(Exception cause);

    @Override public final <R> R accept(Visitor<R, RES> visitor) {
      return visitor.visitSend(this);
    }
  }

  public static abstract class SendStepWithResponse<DATA, RES> implements Step<RES> {
    public abstract WipParamsWithResponse<DATA> getParams();

    /**
     * Handles normal response and returns a next step (or throws {@link ProcessException}).
     */
    public abstract Step<RES> processResponse(DATA response) throws ProcessException;

    /**
     * Optionally wraps the cause with a more high-level exception. Must return cause by default.
     * @return not null
     */
    public abstract Exception processFailure(Exception cause);

    @Override public final <R> R accept(Visitor<R, RES> visitor) {
      return visitor.visitSend(this);
    }
  }

  /**
   * An exception that step can throw if response cannot be processed normally. It aborts the relay
   * and gets passed to user callback.
   */
  public static class ProcessException extends Exception {
    public ProcessException() {
    }
    public ProcessException(String message, Throwable cause) {
      super(message, cause);
    }
    public ProcessException(String message) {
      super(message);
    }
    public ProcessException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Runs a relay defined by a chain of steps.
   * @param <RES> return type of the entire relay
   * @param firstStep a first step that defines the entire relay
   * @param callback
   * @param relaySyncCallback a {@link SyncCallback} wrapped as {@link RelaySyncCallback}
   */
  public static <RES> RelayOk run(final WipCommandProcessor commandProcessor, Step<RES> firstStep,
      final GenericCallback<RES> callback, final RelaySyncCallback relaySyncCallback) {

    return firstStep.accept(new Step.Visitor<RelayOk, RES>() {
      @Override
      public RelayOk visitFinal(RES finalResult) {
        if (callback != null) {
          callback.success(finalResult);
        }
        return relaySyncCallback.finish();
      }

      @Override
      public RelayOk visitSend(final SendStepSimple<RES> sendStep) {
        final RelaySyncCallback.Guard guard = relaySyncCallback.newGuard();

        WipCommandCallback sendCallback = new WipCommandCallback() {
          @Override
          public void messageReceived(WipCommandResponse response) {
            Step<RES> processResult;
            try {
              processResult = sendStep.processResponse();
            } catch (ProcessException e) {
              if (callback != null) {
                callback.failure(e);
              }
              // Todo: consider throwing e.
              return;
            }
            RelayOk relayOk = run(commandProcessor, processResult, callback, guard.getRelay());
            guard.discharge(relayOk);
          }
          @Override
          public void failure(String message) {
            if (callback != null) {
              callback.failure(sendStep.processFailure(new Exception(message)));
            }
          }
        };

        return commandProcessor.send(sendStep.getParams(), sendCallback, guard.asSyncCallback());
      }

      @Override
      public <RESPONSE> RelayOk visitSend(final SendStepWithResponse<RESPONSE, RES> sendStep) {
        final RelaySyncCallback.Guard guard = relaySyncCallback.newGuard();

        GenericCallback<RESPONSE> sendCallback = new GenericCallback<RESPONSE>() {
          @Override
          public void success(RESPONSE response) {
            Step<RES> processResult;
            try {
              processResult = sendStep.processResponse(response);
            } catch (ProcessException e) {
              if (callback != null) {
                callback.failure(e);
              }
              // Todo: consider throwing e.
              return;
            }
            RelayOk relayOk = run(commandProcessor, processResult, callback, guard.getRelay());
            guard.discharge(relayOk);
          }

          @Override
          public void failure(Exception exception) {
            if (callback != null) {
              callback.failure(sendStep.processFailure(exception));
            }
          }
        };

        return commandProcessor.send(sendStep.getParams(), sendCallback, guard.asSyncCallback());
      }
    });
  }
}
