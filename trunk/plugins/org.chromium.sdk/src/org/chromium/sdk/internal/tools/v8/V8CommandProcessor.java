// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.processor.AfterCompileProcessor;
import org.chromium.sdk.internal.tools.v8.processor.BacktraceProcessor;
import org.chromium.sdk.internal.tools.v8.processor.BreakpointProcessor;
import org.chromium.sdk.internal.tools.v8.processor.ContinueProcessor;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONObject;

/**
 * Sends JSON commands to V8 VM and handles responses. Command is sent
 * via {@code V8CommandOutput}. Response is passed back to callback if it was provided.
 * Also all responses and events are dispatched to group of dedicated processors.
 */
public class V8CommandProcessor {

  /**
   * A callback to handle V8 debugger responses.
   */
  public interface V8HandlerCallback {
    /**
     * This method is invoked when a debugger command result has become
     * available.
     *
     * @param response from the V8 debugger
     */
    void messageReceived(JSONObject response);

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

      public void messageReceived(JSONObject response) {
      }
    };
  }

  /** The class logger. */
  static final Logger LOGGER = Logger.getLogger(V8CommandProcessor.class.getName());

  /** The breakpoint processor. */
  private final BreakpointProcessor bpp;

  /** The "afterCompile" event processor. */
  private final AfterCompileProcessor afterCompileProcessor;

  private final Object sendLock = new Object();

  private final V8CommandOutput messageOutput;

  private final BacktraceProcessor backtraceProcessor;

  private final ContinueProcessor continueProcessor;

  private final DebugContextImpl context;

  V8CommandProcessor(V8CommandOutput messageOutput, DebugContextImpl context) {
    this.messageOutput = messageOutput;
    this.context = context;
    this.bpp = new BreakpointProcessor(context);
    this.afterCompileProcessor = new AfterCompileProcessor(context);
    this.backtraceProcessor = new BacktraceProcessor(context);
    this.continueProcessor = new ContinueProcessor(context);
  }


  public DebugContextImpl getContext() {
    return context;
  }

  public BreakpointProcessor getBreakpointProcessor() {
    return bpp;
  }

  public void sendV8CommandAsync(DebuggerMessage message, boolean isImmediate,
      V8CommandProcessor.V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback) {
    synchronized (sendLock) {
      if (isMessageContextDependent(message) && !message.getToken().isValid()) {
        try {
          if (v8HandlerCallback != null) {
            v8HandlerCallback.failure("Invalid context");
          }
        } finally {
          if (syncCallback != null) {
            syncCallback.callbackDone(null);
          }
        }
        return;
      }
      if (DebuggerCommand.CONTINUE.value.equals(message.getCommand())) {
        message.getToken().invalidate();
      }
      if (v8HandlerCallback != null) {
        seqToV8Callbacks.put(message.getSeq(), new CallbackEntry(v8HandlerCallback, syncCallback,
            getCurrentMillis()));
      }
      try {
        messageOutput.send(message, isImmediate);
      } catch (RuntimeException e) {
        if (v8HandlerCallback != null) {
          v8HandlerCallback.failure(e.getMessage());
          seqToV8Callbacks.remove(message.getSeq());
        }
        throw e;
      }
    }
  }

  public void processIncomingJson(final JSONObject v8Json) {
    V8MessageType type = V8MessageType.forString(JsonUtil.getAsString(v8Json, V8Protocol.KEY_TYPE));
    handleResponseWithHandler(type, v8Json);

    if (V8MessageType.RESPONSE == type) {
      final int requestSeq = JsonUtil.getAsLong(v8Json, V8Protocol.KEY_REQSEQ).intValue();
      checkNull(requestSeq, "Could not read 'request_seq' from debugger reply");
      final CallbackEntry callbackEntry = seqToV8Callbacks.remove(requestSeq);
      if (callbackEntry != null) {
        LOGGER.log(
            Level.INFO,
            "Request-response roundtrip: {0}ms",
            getCurrentMillis() - callbackEntry.commitMillis);

        // TODO(prybin): a new thread each time?
        Thread t = new Thread(new Runnable() {
          public void run() {
            try {
              RuntimeException callbackException = null;
              try {
                if (callbackEntry.v8HandlerCallback != null) {
                  LOGGER.log(
                      Level.INFO, "Notified debugger command callback, request_seq={0}", requestSeq);
                  callbackEntry.v8HandlerCallback.messageReceived(v8Json);
                }
              } catch (RuntimeException e) {
                callbackException = e;
              } finally {
                if (callbackEntry.syncCallback != null) {
                  callbackEntry.syncCallback.callbackDone(callbackException);
                }
              }
            } finally {
              // Note that the semaphore is released AFTER the handleResponse
              // call.
              if (callbackEntry.semaphore != null) {
                callbackEntry.semaphore.release();
              }
            }
          }
        });
        t.start();
      }
    }
  }

  /**
   * @param type response type ("response" or "event")
   * @param response from the V8 VM debugger
   */
  private void handleResponseWithHandler(V8MessageType type, final JSONObject response) {
    String commandString = JsonUtil.getAsString(response, V8MessageType.RESPONSE == type
        ? V8Protocol.KEY_COMMAND
        : V8Protocol.KEY_EVENT);
    DebuggerCommand command = DebuggerCommand.forString(commandString);
    if (command == null) {
      LOGGER.log(Level.WARNING,
          "Unknown command in V8 debugger reply JSON: {0}", commandString);
      return;
    }
    final HandlerGetter handlerGetter = command2HandlerGetter.get(command);
    if (handlerGetter == null) {
      return;
    }
    handlerGetter.get(this).messageReceived(response);
  }

  void removeAllCallbacks() {
    for (Iterator<CallbackEntry> it = seqToV8Callbacks.values().iterator(); it.hasNext();) {
      CallbackEntry entry = it.next();
      if (entry.semaphore != null) {
        entry.semaphore.release();
      }
      it.remove();
    }
  }

  private static boolean isMessageContextDependent(DebuggerMessage message) {
    DebuggerCommand command = DebuggerCommand.forString(message.getCommand());
    if (command == null) {
      return false;
    }
    return CONTEXT_DEPENDENT_COMMANDS.contains(command);
  }

  /**
   * @return milliseconds since the epoch
   */
  private static long getCurrentMillis() {
    return System.currentTimeMillis();
  }

  static void checkNull(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }

  private static final EnumSet<DebuggerCommand> CONTEXT_DEPENDENT_COMMANDS = EnumSet.of(
      DebuggerCommand.LOOKUP,
      DebuggerCommand.EVALUATE,
      DebuggerCommand.FRAME);

  /**
   * The callbacks to invoke when the responses arrive.
   */
  private final Map<Integer, CallbackEntry> seqToV8Callbacks =
      new HashMap<Integer, CallbackEntry>();

  private static class CallbackEntry {
    final V8HandlerCallback v8HandlerCallback;

    final long commitMillis;

    final Semaphore semaphore;

    final SyncCallback syncCallback;

    public CallbackEntry(V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback,
        long commitMillis) {
      this(v8HandlerCallback, syncCallback, commitMillis, null);
    }

    public CallbackEntry(V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback,
        long commitMillis, Semaphore semaphore) {
      this.v8HandlerCallback = v8HandlerCallback;
      this.syncCallback = syncCallback;
      this.commitMillis = commitMillis;
      this.semaphore = semaphore;
    }
  }

  private static abstract class HandlerGetter {
    abstract V8CommandProcessor.V8HandlerCallback get(V8CommandProcessor instance);
  }

  /**
   * The handlers that should be invoked when certain command responses arrive.
   */
  private static final Map<DebuggerCommand, HandlerGetter> command2HandlerGetter;
  static {
    command2HandlerGetter = new HashMap<DebuggerCommand, HandlerGetter>();
    HandlerGetter bppGetter = new HandlerGetter() {
      @Override
      BreakpointProcessor get(V8CommandProcessor instance) {
        return instance.bpp;
      }
    };
    command2HandlerGetter.put(DebuggerCommand.CHANGEBREAKPOINT, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.SETBREAKPOINT, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.CLEARBREAKPOINT, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.BREAK /* event */, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.EXCEPTION /* event */, bppGetter);

    command2HandlerGetter.put(DebuggerCommand.AFTER_COMPILE /* event */,
        new HandlerGetter() {
      @Override
      AfterCompileProcessor get(V8CommandProcessor instance) {
        return instance.afterCompileProcessor;
      }
    });

    command2HandlerGetter.put(DebuggerCommand.BACKTRACE,
        new HandlerGetter() {
      @Override
      BacktraceProcessor get(V8CommandProcessor instance) {
        return instance.backtraceProcessor;
      }
    });

    command2HandlerGetter.put(DebuggerCommand.CONTINUE,
        new HandlerGetter() {
      @Override
      ContinueProcessor get(V8CommandProcessor instance) {
        return instance.continueProcessor;
      }
    });
  }
}
