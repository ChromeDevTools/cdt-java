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
import org.chromium.sdk.internal.wip.protocol.input.InspectedUrlChangedData;
import org.chromium.sdk.internal.wip.protocol.input.ParsedScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.input.PausedScriptData;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Success;
import org.chromium.sdk.internal.wip.protocol.input.WipEvent;
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
    String eventName = event.event();
    EventHandler eventHandler = EVENT_HANDLERS.get(eventName);
    if (eventHandler == null) {
      LOGGER.log(Level.SEVERE, "Unsupported event: " + eventName);
      return;
    }
    eventHandler.accept(event, this);
  }

  /**
   * Handles all operations specific to Wip messages.
   */
  private class WipMessageTypeHandler implements
      BaseCommandProcessor.Handler<Integer, JSONObject, JSONObject, WipCommandResponse> {
    @Override
    public Integer getUpdatedSeq(JSONObject message) {
      Integer seq = currentSeq.addAndGet(1);
      message.put(BasicConstants.Property.SEQ, seq);
      return seq;
    }

    @Override
    public String getCommandName(JSONObject message) {
      return (String) message.get(BasicConstants.Property.COMMAND);
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
      if (incoming.get(BasicConstants.Property.EVENT) != null) {
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
      Object seqObject = incomingWithSeq.seq();
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

  private abstract static class EventHandler {
    abstract void accept(WipEvent yurysEvent, WipCommandProcessor commandProcessor);
  }

  private static final Map<String, EventHandler> EVENT_HANDLERS;
  static {
    EVENT_HANDLERS = new HashMap<String, EventHandler>();
    EVENT_HANDLERS.put("inspectedURLChanged", new EventHandler() {
      @Override
      void accept(WipEvent yurysEvent, WipCommandProcessor commandProcessor) {

        InspectedUrlChangedData urlData;
        try {
          urlData = yurysEvent.data().asInspectedUrlChangedData();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }
        commandProcessor.tabImpl.getScriptManager().pageReloaded();
        commandProcessor.tabImpl.updateUrl(urlData.url());
      }
    });
    EventHandler noOpHandler = new EventHandler() {
      @Override
      void accept(WipEvent yurysEvent, WipCommandProcessor commandProcessor) {
      }
    };
    EVENT_HANDLERS.put("bringToFront", noOpHandler);
    EVENT_HANDLERS.put("showPanel", noOpHandler);
    EVENT_HANDLERS.put("profilerWasEnabled", noOpHandler);
    EVENT_HANDLERS.put("debuggerWasEnabled", noOpHandler);
    EVENT_HANDLERS.put("updateResource", noOpHandler);
    EVENT_HANDLERS.put("setDocument", noOpHandler);
    EVENT_HANDLERS.put("addDOMStorage", noOpHandler);
    EVENT_HANDLERS.put("parsedScriptSource", new EventHandler() {
      @Override
      void accept(WipEvent yurysEvent, final WipCommandProcessor commandProcessor) {
        ParsedScriptSourceData data;
        try {
          data = yurysEvent.data().asParsedScriptSourceData();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }

        commandProcessor.tabImpl.getScriptManager().scriptIsReportedParsed(data);
      }
    });
    EVENT_HANDLERS.put("restoredBreakpoint", noOpHandler);
    EVENT_HANDLERS.put("pausedScript", new EventHandler() {
      @Override
      void accept(WipEvent event, WipCommandProcessor commandProcessor) {
        PausedScriptData data;
        try {
          data = event.data().asPausedScriptData();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }

        commandProcessor.tabImpl.getContextBuilder().createContext(data);
      }
    });
    EVENT_HANDLERS.put("resumedScript", new EventHandler() {
      @Override
      void accept(WipEvent event, final WipCommandProcessor commandProcessor) {
        commandProcessor.tabImpl.getContextBuilder().onResumeReportedFromRemote();
      }
    });
  }

  public void runInDispatchThread(Runnable runnable) {
    this.tabImpl.getWsSocket().runInDispatchThread(runnable);
  }
}
