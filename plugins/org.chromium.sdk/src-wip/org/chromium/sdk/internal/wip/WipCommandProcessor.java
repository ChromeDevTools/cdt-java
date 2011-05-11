// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugEventListener.VmStatusListener;
import org.chromium.sdk.JavascriptVm.GenericCallback;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.BaseCommandProcessor;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.wip.protocol.BasicConstants;
import org.chromium.sdk.internal.wip.protocol.WipParserAccess;
import org.chromium.sdk.internal.wip.protocol.WipProtocol;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Success;
import org.chromium.sdk.internal.wip.protocol.input.WipEvent;
import org.chromium.sdk.internal.wip.protocol.input.WipEventType;
import org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData;
import org.chromium.sdk.internal.wip.protocol.output.WipParams;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.internal.wip.protocol.output.WipRequest;
import org.json.simple.JSONObject;

/**
 * Responsible for the basic processing and dispatching all incoming and outgoing messages.
 */
class WipCommandProcessor {
  private static final Logger LOGGER = Logger.getLogger(WipCommandProcessor.class.getName());

  private final WipTabImpl tabImpl;
  private final BaseCommandProcessor<Integer, JSONObject, JSONObject, WipCommandResponse>
      baseProcessor;
  private final AtomicInteger currentSeq = new AtomicInteger(0);

  WipCommandProcessor(WipTabImpl tabImpl, WsConnection wsSocket) {
    this.tabImpl = tabImpl;

    WipMessageTypeHandler handler = new WipMessageTypeHandler();

    baseProcessor =
        new BaseCommandProcessor<Integer, JSONObject, JSONObject, WipCommandResponse>(handler);
  }
  // TODO: inline this method.
  void send(JSONObject message, WipCommandCallback callback, SyncCallback syncCallback) {
    sendRaw(message, callback, syncCallback);
  }

  void sendRaw(JSONObject message, WipCommandCallback callback, SyncCallback syncCallback) {
    baseProcessor.send(message, false, callback, syncCallback);
  }

  void send(WipParams params, WipCommandCallback callback, SyncCallback syncCallback) {
    WipRequest request = new WipRequest(params);
    sendRaw(request, callback, syncCallback);
  }

  /**
   * @param <RESPONSE> type of response expected that is determined by params
   * @param params request parameters that also holds a method name
   * @param callback a callback that accepts method-specific response or null
   * @param syncCallback may be null
   */
  <RESPONSE> void send(final WipParamsWithResponse<RESPONSE> params,
      final GenericCallback<RESPONSE> callback, SyncCallback syncCallback) {
    WipRequest request = new WipRequest(params);

    WipCommandCallback commandCallback;
    if (callback == null) {
      commandCallback = null;
    } else {
      commandCallback = new WipCommandCallback.Default() {
        @Override
        protected void onSuccess(Success success) {
          RESPONSE response;
          try {
            response = params.parseResponse(success.data(), WipParserAccess.get());
          } catch (JsonProtocolParseException e) {
            throw new RuntimeException(e);
          }
          callback.success(response);
        }

        @Override
        protected void onError(String message) {
          callback.failure(new Exception(message));
        }
      };
    }

    sendRaw(request, commandCallback, syncCallback);
  }

  void acceptResponse(JSONObject message) {
    baseProcessor.processIncoming(message);
  }

  private void processEvent(JSONObject jsonObject) {
    WipEvent event;
    try {
      event = WipProtocol.getParser().parse(jsonObject, WipEvent.class);
    } catch (JsonProtocolParseException e) {
      LOGGER.log(Level.SEVERE, "Failed to parse event", e);
      return;
    }
    String eventName = event.method();
    EVENT_MAP.handleEvent(event, this);
  }

  /**
   * Handles all operations specific to Wip messages.
   */
  private class WipMessageTypeHandler implements
      BaseCommandProcessor.Handler<Integer, JSONObject, JSONObject, WipCommandResponse> {
    @Override
    public Integer getUpdatedSeq(JSONObject message) {
      Integer seq = currentSeq.addAndGet(1);
      message.put(BasicConstants.Property.ID, seq);
      return seq;
    }

    @Override
    public String getCommandName(JSONObject message) {
      return (String) message.get(BasicConstants.Property.METHOD);
    }

    @Override
    public void send(JSONObject message, boolean isImmediate) {
      try {
        WipCommandProcessor.this.tabImpl.getWsSocket().sendTextualMessage(
            message.toJSONString());
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to send", e);
      }
    }

    @Override
    public WipCommandResponse parseWithSeq(JSONObject incoming) {
      if (!incoming.containsKey(BasicConstants.Property.ID)) {
        return null;
      }
      try {
        return WipProtocol.getParser().parse(incoming, WipCommandResponse.class);
      } catch (JsonProtocolParseException e) {
        throw new RuntimeException("Failed to parse response", e);
      }
    }

    @Override
    public Integer getSeq(WipCommandResponse incomingWithSeq) {
      Object seqObject = incomingWithSeq.id();
      if (seqObject == null) {
        return null;
      }
      Number number = (Number) seqObject;
      return number.intValue();
    }

    @Override
    public void acceptNonSeq(JSONObject incoming) {
      processEvent(incoming);
    }

    @Override
    public void reportVmStatus(String currentRequest, int numberOfEnqueued) {
      TabDebugEventListener tabEventListener = tabImpl.getDebugListener();
      VmStatusListener vmStatusListener =
          tabEventListener.getDebugEventListener().getVmStatusListener();
      if (vmStatusListener != null) {
        vmStatusListener.busyStatusChanged(currentRequest, numberOfEnqueued);
      }
    }
  }

  private abstract static class EventHandler<T> {
    abstract void accept(T eventData, WipCommandProcessor commandProcessor);
  }

  private static final EventMap EVENT_MAP;
  static {
    EVENT_MAP = new EventMap();

    // TODO: support url changed event.

    EVENT_MAP.add(PausedEventData.TYPE, new EventHandler<PausedEventData>() {
      @Override
      void accept(PausedEventData data, WipCommandProcessor commandProcessor) {
        commandProcessor.tabImpl.getContextBuilder().createContext(data);
      }
    });
    EVENT_MAP.add(ResumedEventData.TYPE, new EventHandler<ResumedEventData>() {
      @Override
      void accept(ResumedEventData event, WipCommandProcessor commandProcessor) {
        commandProcessor.tabImpl.getContextBuilder().onResumeReportedFromRemote(event);
      }
    });
    EVENT_MAP.add(ScriptParsedEventData.TYPE, new EventHandler<ScriptParsedEventData> () {
      @Override
      void accept(ScriptParsedEventData eventData,
          WipCommandProcessor commandProcessor) {
        commandProcessor.tabImpl.getScriptManager().scriptIsReportedParsed(eventData);
      }
    });
  }

  public void runInDispatchThread(Runnable runnable) {
    this.tabImpl.getWsSocket().runInDispatchThread(runnable);
  }

  private static class EventMap {
    private final Map<String, InternalHandler<?>> map = new HashMap<String, InternalHandler<?>>();

    public <T> void add(WipEventType<T> type, EventHandler<T> eventHandler) {
      InternalHandler<T> internalHandler = new InternalHandler<T>(eventHandler, type);
      map.put(type.getMethodName(), internalHandler);
    }

    public void handleEvent(WipEvent event, WipCommandProcessor commandProcessor) {
      String method = event.method();
      InternalHandler<?> parser = map.get(method);
      if (parser == null) {
        LOGGER.log(Level.INFO, "Unsupported event: " + method);
        return;
      }
      parser.handle(event, commandProcessor);
    }

    private static class InternalHandler<T> {
      private final EventHandler<T> handler;
      private final WipEventType<T> type;

      InternalHandler(EventHandler<T> handler, WipEventType<T> type) {
        this.handler = handler;
        this.type = type;
      }

      public void handle(WipEvent event, WipCommandProcessor commandProcessor) {
        if (handler == null) {
          return;
        }
        T data;
        try {
          data = WipParserAccess.get().parse(event.data().getUnderlyingObject(),
              type.getEventType());
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }
        handler.accept(data, commandProcessor);
      }
    }
  }
}
