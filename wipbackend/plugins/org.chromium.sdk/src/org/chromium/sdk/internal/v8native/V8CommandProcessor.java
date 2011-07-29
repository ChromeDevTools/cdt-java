// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.BaseCommandProcessor;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.protocol.input.CommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.IncomingMessage;
import org.chromium.sdk.internal.v8native.protocol.input.V8ProtocolParserAccess;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.json.simple.JSONObject;

/**
 * Sends JSON commands to V8 VM and handles responses. Command is sent
 * via {@code V8CommandOutput}. Response is passed back to callback if it was provided.
 * Also all responses and events are dispatched to group of dedicated processors.
 */
public class V8CommandProcessor implements V8CommandSender<DebuggerMessage, RuntimeException> {
  /**
   * A callback to handle V8 debugger responses.
   */
  public interface V8HandlerCallback extends BaseCommandProcessor.Callback<CommandResponse> {
    /**
     * This method is invoked when a debugger command result has become
     * available.
     *
     * @param response from the V8 debugger
     */
    void messageReceived(CommandResponse response);

    /**
     * This method is invoked when a debugger command has failed.
     *
     * @param message containing the failure reason
     */
    void failure(String message);

    /** A no-op callback implementation. */
    V8HandlerCallback NULL_CALLBACK = new V8HandlerCallback() {
      public void failure(String message) {
      }

      public void messageReceived(CommandResponse response) {
      }
    };
  }

  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(V8CommandProcessor.class.getName());

  private final V8CommandOutput messageOutput;

  private final DefaultResponseHandler defaultResponseHandler;

  private final DebugSession debugSession;

  private final BaseCommandProcessor<Integer, DebuggerMessage, IncomingMessage, CommandResponse>
      baseCommandProcessor;


  public V8CommandProcessor(V8CommandOutput messageOutput,
      DefaultResponseHandler defaultResponseHandler, DebugSession debugSession) {
    this.messageOutput = messageOutput;
    this.defaultResponseHandler = defaultResponseHandler;
    this.debugSession = debugSession;
    this.baseCommandProcessor =
        new BaseCommandProcessor<Integer, DebuggerMessage, IncomingMessage, CommandResponse>(
            new HandlerImpl());
  }

  @Override
  public RelayOk sendV8CommandAsync(DebuggerMessage message, boolean isImmediate,
      V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback) {
    return baseCommandProcessor.send(message, isImmediate, v8HandlerCallback, syncCallback);
  }

  public RelayOk runInDispatchThread(final Runnable callback, final SyncCallback syncCallback) {
    Runnable innerRunnable = new Runnable() {
      @Override public void run() {
        RuntimeException exception = null;
        try {
          callback.run();
        } catch (RuntimeException e) {
          exception = e;
          throw e;
        } finally {
          if (syncCallback != null) {
            syncCallback.callbackDone(exception);
          }
        }
      }
    };
    messageOutput.runInDispatchThread(innerRunnable);
    return DISPATCH_THREAD_PROMISES_TO_CALL;
  }

  public void processIncomingJson(final JSONObject v8Json) {
    IncomingMessage response;
    try {
      response = V8ProtocolParserAccess.get().parse(v8Json, IncomingMessage.class);
    } catch (JsonProtocolParseException e) {
      LOGGER.log(Level.SEVERE, "JSON message does not conform to the protocol", e);
      return;
    }
    baseCommandProcessor.processIncoming(response);
  }

  public void processEos() {
    baseCommandProcessor.processEos();
  }

  private class HandlerImpl implements
      BaseCommandProcessor.Handler<Integer, DebuggerMessage, IncomingMessage, CommandResponse> {
    public Integer getUpdatedSeq(DebuggerMessage message) {
      return message.getSeq();
    }

    public String getCommandName(DebuggerMessage message) {
      return message.getCommand();
    }

    public void send(DebuggerMessage message, boolean isImmediate) {
      V8CommandProcessor.this.messageOutput.send(message, isImmediate);
    }

    public CommandResponse parseWithSeq(IncomingMessage incoming) {
      return incoming.asCommandResponse();
    }

    public Integer getSeq(CommandResponse incomingWithSeq) {
      return (int) incomingWithSeq.requestSeq();
    }

    public void acceptNonSeq(IncomingMessage incoming) {
      V8CommandProcessor.this.defaultResponseHandler.handleResponseWithHandler(incoming);
    }

    public void reportVmStatus(String currentRequest, int numberOfEnqueued) {
      DebugEventListener.VmStatusListener statusListener =
        V8CommandProcessor.this.debugSession.getDebugEventListener().getVmStatusListener();
      if (statusListener == null) {
        return;
      }
    }
  }

  private static final RelayOk DISPATCH_THREAD_PROMISES_TO_CALL = new RelayOk() {};
}
