// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;

/**
 * Provides basic command processor functionality: sends/receives commands/events and
 * supports callbacks for commands. It also supports status reporting for UI.
 * All operations such as sending/receiving/parsing are implemented by a {@link Handler}.
 *
 * @param <SEQ_KEY> type of command sequence number key
 * @param <OUTGOING> type of outgoing message
 * @param <INCOMING> type of incoming message
 * @param <INCOMING_WITH_SEQ> type of incomming message that is a command (has sequence number)
 */
public class BaseCommandProcessor<SEQ_KEY, OUTGOING, INCOMING, INCOMING_WITH_SEQ> {
  private static final Logger LOGGER = Logger.getLogger(BaseCommandProcessor.class.getName());

  public interface Handler<SEQ_KEY, OUTGOING, INCOMING, INCOMING_WITH_SEQ> {
    SEQ_KEY getUpdatedSeq(OUTGOING message);
    String getCommandName(OUTGOING message);
    void send(OUTGOING message, boolean isImmediate);
    INCOMING_WITH_SEQ parseWithSeq(INCOMING incoming);
    SEQ_KEY getSeq(INCOMING_WITH_SEQ incomingWithSeq);
    void acceptNonSeq(INCOMING incoming);
    void reportVmStatus(String currentRequest, int numberOfEnqueued);
  }

  /**
   * @param <INCOMING_WITH_SEQ>
   */
  public interface Callback<INCOMING_WITH_SEQ> {
    void messageReceived(INCOMING_WITH_SEQ response);
    void failure(String message);
  }

  private final CloseableMap<SEQ_KEY, CallbackEntry<INCOMING_WITH_SEQ>> callbackMap =
      CloseableMap.newLinkedMap();
  private final Handler<SEQ_KEY, OUTGOING, INCOMING, INCOMING_WITH_SEQ> handler;

  public BaseCommandProcessor(
      Handler<SEQ_KEY, OUTGOING, INCOMING, INCOMING_WITH_SEQ> handler) {
    this.handler = handler;
  }

  public RelayOk send(OUTGOING message, boolean isImmediate,
      Callback<? super INCOMING_WITH_SEQ> callback, SyncCallback syncCallback) {
    SEQ_KEY seq = handler.getUpdatedSeq(message);
    boolean callbackAdded;
    if (callback != null || syncCallback != null) {
      String commandName = handler.getCommandName(message);

      try {
        callbackMap.put(seq,
            new CallbackEntry<INCOMING_WITH_SEQ>(callback, syncCallback, commandName));
      } catch (IllegalStateException e) {
        throw new IllegalStateException("Connection is closed", e);
      }
      callbackAdded = true;
      reportVmStatus();
    } else {
      callbackAdded = false;
    }
    try {
      handler.send(message, isImmediate);
    } catch (RuntimeException e) {
      if (callbackAdded) {
        callbackMap.remove(seq);
      }
      throw e;
    }
    return WE_SENT_IT_RELAY_OK;
  }

  public void processIncoming(INCOMING incomingParsed) {
    final INCOMING_WITH_SEQ commandResponse = handler.parseWithSeq(incomingParsed);

    if (commandResponse != null) {
      SEQ_KEY key = handler.getSeq(commandResponse);
      CallbackEntry<INCOMING_WITH_SEQ> callbackEntry = callbackMap.removeIfContains(key);
      if (callbackEntry != null) {
        LOGGER.log(
            Level.FINE,
            "Request-response roundtrip: {0}ms",
            getCurrentMillis() - callbackEntry.commitMillis);
        reportVmStatus();

        CallbackCaller<Callback<? super INCOMING_WITH_SEQ>> caller =
            new CallbackCaller<Callback<? super INCOMING_WITH_SEQ>>() {
          @Override
          void call(Callback<? super INCOMING_WITH_SEQ> handlerCallback) {
            handlerCallback.messageReceived(commandResponse);
          }
        };
        try {
          callThemBack(callbackEntry, caller, key);
        } catch (RuntimeException e) {
          LOGGER.log(Level.SEVERE, "Failed to dispatch response to callback", e);
        }
      }
    } else {
      handler.acceptNonSeq(incomingParsed);
    }
  }

  public void processEos() {
    // We should call them in the order they have been submitted.
    Collection<CallbackEntry<INCOMING_WITH_SEQ>> entries = callbackMap.close().values();
    for (CallbackEntry<INCOMING_WITH_SEQ> entry : entries) {
      try {
        callThemBack(entry, failureCaller, null);
      } catch (RuntimeException e) {
        LOGGER.log(Level.SEVERE, "Failed to dispatch response to callback", e);
      }
    }
  }

  private void callThemBack(CallbackEntry<INCOMING_WITH_SEQ> callbackEntry,
      CallbackCaller<? super Callback<? super INCOMING_WITH_SEQ>> callbackCaller,
      SEQ_KEY requestSeq) {
    RuntimeException callbackException = null;
    try {
      if (callbackEntry.callback != null) {
        LOGGER.log(
            Level.FINE, "Notified debugger command callback, request_seq={0}", requestSeq);
        callbackCaller.call(callbackEntry.callback);
      }
    } catch (RuntimeException e) {
      callbackException = e;
      throw e;
    } finally {
      if (callbackEntry.syncCallback != null) {
        callbackEntry.syncCallback.callbackDone(callbackException);
      }
    }
  }

  private static abstract class CallbackCaller<CALLBACK> {
    abstract void call(CALLBACK handlerCallback);
  }

  private final CallbackCaller<Callback<?>> failureCaller = new CallbackCaller<Callback<?>>() {
    @Override
    void call(Callback<?> handlerCallback) {
      handlerCallback.failure("Connection closed");
    }
  };

  private static class CallbackEntry<INCOMING_WITH_SEQ> {
    final Callback<? super INCOMING_WITH_SEQ> callback;

    final SyncCallback syncCallback;

    final long commitMillis;

    final String requestName;

    CallbackEntry(Callback<? super INCOMING_WITH_SEQ> callback, SyncCallback syncCallback,
        String requestName) {
      this.callback = callback;
      this.commitMillis = getCurrentMillis();
      this.syncCallback = syncCallback;
      this.requestName = requestName;
    }
  }

  /**
   * @return milliseconds since the epoch
   */
  private static long getCurrentMillis() {
    return System.currentTimeMillis();
  }

  private final Object vmStatusReportMonitor = new Object();
  private void reportVmStatus() {
    // We synchronize, because one thread may be delivering obsolete message while a more
    // recent message has already been delivered by other thread.
    synchronized (vmStatusReportMonitor) {
      int size = callbackMap.size();
      CallbackEntry<?> firstEntry = callbackMap.peekFirst();
      // Those 2 variables above might be not in synch, so for a brief moment user may see
      // a wrong message (when size == 0 and firstEntry is null). This is OK.
      if (firstEntry == null) {
        handler.reportVmStatus(null, 0);
      } else {
        handler.reportVmStatus(firstEntry.requestName, size - 1);
      }
    }
  }

  private static final RelayOk WE_SENT_IT_RELAY_OK = new RelayOk() {};
}
