// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugEventListener.VmStatusListener;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.rynda.protocol.BasicConstants;
import org.chromium.sdk.internal.rynda.protocol.RyndaProtocol;
import org.chromium.sdk.internal.rynda.protocol.input.InspectedUrlChangedData;
import org.chromium.sdk.internal.rynda.protocol.input.ParsedScriptSourceData;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaCommandResponse;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaEvent;
import org.chromium.sdk.internal.tools.v8.BaseCommandProcessor;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.json.simple.JSONObject;

/**
 * Responsible for the basic processing and dispatching all incoming and outgoing messages.
 */
class RyndaCommandProcessor {
  private static final Logger LOGGER = Logger.getLogger(RyndaCommandProcessor.class.getName());

  private final RyndaTabImpl tabImpl;
  private final BaseCommandProcessor<Integer, JSONObject, JSONObject, RyndaCommandResponse>
      baseProcessor;
  private final AtomicInteger currentSeq = new AtomicInteger(0);

  RyndaCommandProcessor(RyndaTabImpl tabImpl, WsConnection wsSocket) {
    this.tabImpl = tabImpl;

    RyndaMessageTypeHandler handler = new RyndaMessageTypeHandler();

    baseProcessor =
        new BaseCommandProcessor<Integer, JSONObject, JSONObject, RyndaCommandResponse>(handler);
  }

  void send(JSONObject message, RyndaCommandCallback callback, SyncCallback syncCallback) {
    baseProcessor.send(message, false, callback, syncCallback);
  }

  void acceptResponse(JSONObject message) {
    baseProcessor.processIncoming(message);
  }

  private void processEvent(JSONObject jsonObject) {
    RyndaEvent event;
    try {
      event = RyndaProtocol.PARSER.parse(jsonObject, RyndaEvent.class);
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
   * Handles all operations specific to rynda messages.
   */
  private class RyndaMessageTypeHandler implements
      BaseCommandProcessor.Handler<Integer, JSONObject, JSONObject, RyndaCommandResponse> {
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
        RyndaCommandProcessor.this.tabImpl.getWsSocket().sendTextualMessage(
            message.toJSONString());
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to send", e);
      }
    }

    @Override
    public RyndaCommandResponse parseWithSeq(JSONObject incoming) {
      if (incoming.get(BasicConstants.Property.EVENT) != null) {
        return null;
      }
      try {
        return RyndaProtocol.PARSER.parse(incoming, RyndaCommandResponse.class);
      } catch (JsonProtocolParseException e) {
        throw new RuntimeException("Failed to parse response", e);
      }
    }

    @Override
    public Integer getSeq(RyndaCommandResponse incomingWithSeq) {
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
    abstract void accept(RyndaEvent yurysEvent, RyndaCommandProcessor commandProcessor);
  }

  private static final Map<String, EventHandler> EVENT_HANDLERS;
  static {
    EVENT_HANDLERS = new HashMap<String, EventHandler>();
    EVENT_HANDLERS.put("inspectedURLChanged", new EventHandler() {
      @Override
      void accept(RyndaEvent yurysEvent, RyndaCommandProcessor commandProcessor) {

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
      void accept(RyndaEvent yurysEvent, RyndaCommandProcessor commandProcessor) {
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
      void accept(RyndaEvent yurysEvent, final RyndaCommandProcessor commandProcessor) {
        ParsedScriptSourceData data;
        try {
          data = yurysEvent.data().asParsedScriptSourceData();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }

        commandProcessor.tabImpl.getScriptManager().scriptIsReportedParsed(data);
      }
    });
  }

  public void runInDispatchThread(Runnable runnable) {
    this.tabImpl.getWsSocket().runInDispatchThread(runnable);
  }
}
