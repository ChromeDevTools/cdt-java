// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.internal.BrowserImpl;
import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.MessageFactory;
import org.chromium.sdk.internal.Result;
import org.chromium.sdk.internal.tools.ChromeDevToolsProtocol;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.tools.v8.processor.AfterCompileProcessor;
import org.chromium.sdk.internal.tools.v8.processor.BacktraceProcessor;
import org.chromium.sdk.internal.tools.v8.processor.BreakpointProcessor;
import org.chromium.sdk.internal.tools.v8.processor.ContinueProcessor;
import org.chromium.sdk.internal.tools.v8.processor.V8ResponseCallback;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Message;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Handles the interaction with the "V8Debugger" tool.
 */
public class V8DebuggerToolHandler implements ToolHandler {

  /**
   * This exception is thrown whenever the handler could not get a tab
   * attachment result from the debugged browser.
   */
  public static class AttachmentFailureException extends Exception {

    private static final long serialVersionUID = 1L;

    public AttachmentFailureException() {
    }

    public AttachmentFailureException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * An interface to run callbacks in response to V8 debugger commands that do
   * not have associated JSON payloads.
   */
  public interface ResultAwareCallback {

    /**
     * This method is invoked whenever a response to a V8 command is received.
     *
     * @param result of the command
     */
    void resultReceived(Result result);
  }

  private static class CallbackEntry {
    final BrowserTabImpl.V8HandlerCallback v8HandlerCallback;

    final long commitMillis;

    final Semaphore semaphore;

    public CallbackEntry(BrowserTabImpl.V8HandlerCallback v8HandlerCallback, long commitMillis) {
      this(v8HandlerCallback, commitMillis, null);
    }

    public CallbackEntry(BrowserTabImpl.V8HandlerCallback v8HandlerCallback, long commitMillis,
        Semaphore semaphore) {
      this.v8HandlerCallback = v8HandlerCallback;
      this.commitMillis = commitMillis;
      this.semaphore = semaphore;
    }
  }

  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(V8DebuggerToolHandler.class.getName());

  private static final EnumSet<DebuggerCommand> CONTEXT_DEPENDENT_COMMANDS = EnumSet.of(
      DebuggerCommand.LOOKUP,
      DebuggerCommand.EVALUATE,
      DebuggerCommand.FRAME);

  /**
   * The callbacks to invoke when the responses arrive.
   */
  private final Map<Integer, CallbackEntry> seqToV8Callbacks =
      new HashMap<Integer, CallbackEntry>();

  /**
   * The handlers that should be invoked when certain command responses arrive.
   */
  private final Map<DebuggerCommand, V8ResponseCallback> commandToHandlerMap =
      new HashMap<DebuggerCommand, V8ResponseCallback>();

  /** The breakpoint processor. */
  private final BreakpointProcessor bpp;

  /** The "afterCompile" event processor. */
  private final AfterCompileProcessor afterCompileProcessor;

  /** The host BrowserImpl instance. */
  private final BrowserImpl browserImpl;

  /** The debug context for this handler. */
  private final DebugContextImpl context;

  /** A synchronization object for the field access/modification. */
  private final Object fieldAccessLock = new Object();

  // The fields access is synchronized
  private boolean isAttached;

  private ResultAwareCallback attachCallback;

  private ResultAwareCallback detachCallback;

  private final Object sendLock = new Object();

  /**
   * A no-op JavaScript to evaluate.
   */
  public static final String JAVASCRIPT_VOID = "javascript:void(0);";

  public V8DebuggerToolHandler(BrowserImpl browserImpl, DebugContextImpl context) {
    this.browserImpl = browserImpl;
    this.context = context;
    this.bpp = new BreakpointProcessor(context);
    this.afterCompileProcessor = new AfterCompileProcessor(context);

    commandToHandlerMap.put(DebuggerCommand.CHANGEBREAKPOINT, bpp);
    commandToHandlerMap.put(DebuggerCommand.SETBREAKPOINT, bpp);
    commandToHandlerMap.put(DebuggerCommand.CLEARBREAKPOINT, bpp);
    commandToHandlerMap.put(DebuggerCommand.BREAK /* event */, bpp);
    commandToHandlerMap.put(DebuggerCommand.EXCEPTION /* event */, bpp);

    commandToHandlerMap.put(DebuggerCommand.AFTER_COMPILE /* event */, afterCompileProcessor);

    commandToHandlerMap.put(DebuggerCommand.BACKTRACE, new BacktraceProcessor(context));

    commandToHandlerMap.put(DebuggerCommand.CONTINUE, new ContinueProcessor(context));
  }

  public DebugEventListener getDebugEventListener() {
    return context.getTab().getDebugEventListener();
  }

  public BreakpointProcessor getBreakpointProcessor() {
    return bpp;
  }

  public void handleMessage(final Message message) {
    JSONObject json;
    try {
      json = JsonUtil.jsonObjectFromJson(message.getContent());
    } catch (ParseException e) {
      LOGGER.log(Level.SEVERE, "Invalid JSON received: " + message.getContent());
      return;
    }
    String commandString = JsonUtil.getAsString(json, ChromeDevToolsProtocol.COMMAND.key);
    DebuggerToolCommand command = DebuggerToolCommand.forName(commandString);
    if (command != null) {
      switch (command) {
        case ATTACH:
          processAttach(json);
          break;
        case DETACH:
          processDetach(json);
          break;
        case DEBUGGER_COMMAND:
          processDebuggerCommand(json);
          break;
        case NAVIGATED:
          processNavigated(json);
          break;
        case CLOSED:
          processClosed(json);
          break;
      }
      return;
    }
    throw new IllegalArgumentException("Invalid command: " + commandString);
  }

  public void onDebuggerDetached() {
    if (!isAttached()) {
      // We've already been notified.
      return;
    }
    synchronized (fieldAccessLock) {
      isAttached = false;
    }
    removeAllCallbacks();
    DebugEventListener debugEventListener = getDebugEventListener();
    if (debugEventListener != null) {
      debugEventListener.disconnected();
    }
    context.getTab().sessionTerminated();
  }

  public int getAttachedTab() {
    if (!isAttached()) {
      throw new IllegalStateException("Debugger is not attached to any tab");
    }
    return context.getTabId();
  }

  /**
   * @return whether the handler is attached to a tab
   */
  public boolean isAttached() {
    synchronized (fieldAccessLock) {
      return isAttached;
    }
  }

  public Exception sendV8CommandBlocking(
      DebuggerMessage message, BrowserTabImpl.V8HandlerCallback v8HandlerCallback) {
    BlockingV8RequestCommand command =
        new BlockingV8RequestCommand(this, message, true, v8HandlerCallback);
    command.run();
    return command.getException();
  }

  public void sendV8Command(DebuggerMessage message,
      boolean isImmediate, BrowserTabImpl.V8HandlerCallback v8HandlerCallback) {
    synchronized (sendLock) {
      if (isMessageContextDependent(message) && !message.getToken().isValid()) {
        if (v8HandlerCallback != null) {
          v8HandlerCallback.failure("Invalid context");
        }
        return;
      }
      if (DebuggerCommand.CONTINUE.value.equals(message.getCommand())) {
        message.getToken().invalidate();
      }
      if (v8HandlerCallback != null) {
        seqToV8Callbacks.put(message.getSeq(), new CallbackEntry(v8HandlerCallback,
            getCurrentMillis()));
      }
      try {
        String destination = String.valueOf(getAttachedTab());
        getConnection().send(
            MessageFactory.debuggerCommand(
                destination,
                JsonUtil.streamAwareToJson(message)));
        if (isImmediate) {
          getConnection().send(
              MessageFactory.evaluateJavascript(destination, JAVASCRIPT_VOID));
        }
      } catch (RuntimeException e) {
        if (v8HandlerCallback != null) {
          v8HandlerCallback.failure(e.getMessage());
          seqToV8Callbacks.remove(message.getSeq());
        }
        throw e;
      }
    }
  }

  /**
   * Attaches the remote debugger to the associated browser tab.
   *
   * @return the attachment result
   * @throws AttachmentFailureException whenever the handler could not connect
   *         to the browser
   */
  public Result attachToTab() throws AttachmentFailureException {
    if (isAttached()) {
      return Result.ILLEGAL_TAB_STATE;
    }
    final Semaphore sem = new Semaphore(0);
    final Result[] output = new Result[1];
    synchronized (fieldAccessLock) {
      this.attachCallback = new ResultAwareCallback() {
        public void resultReceived(Result result) {
          output[0] = result;
          sem.release();
        }
      };
    }
    // No guarding against invocation while an "attach" command is in flight.
    getConnection().send(MessageFactory.attach(String.valueOf(context.getTabId())));

    // If the attachment fails, notify the listener disconnected() method.
    try {
      boolean attached = sem.tryAcquire(BrowserImpl.OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      if (!attached) {
        throw new AttachmentFailureException("Timed out", null);
      }
    } catch (InterruptedException e) {
      throw new AttachmentFailureException(null, e);
    }

    return output[0];
  }

  /**
   * Detaches the remote debugger from the associated browser tab.
   *
   * @return the detachment result
   */
  public Result detachFromTab() {
    if (!isAttached()) {
      onDebuggerDetached();
      return Result.ILLEGAL_TAB_STATE;
    }
    final Semaphore sem = new Semaphore(0);
    final Result[] output = new Result[1];
    synchronized (fieldAccessLock) {
      detachCallback = new ResultAwareCallback() {
        public void resultReceived(Result result) {
          output[0] = result;
          sem.release();
        }
      };
    }
    // No guarding against invocation while a "detach" command is in flight.
    getConnection().send(MessageFactory.detach(String.valueOf(context.getTabId())));

    try {
      sem.tryAcquire(BrowserImpl.OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Fall through
    }
    return output[0];
  }

  /**
   * @return milliseconds since the epoch
   */
  protected long getCurrentMillis() {
    return System.currentTimeMillis();
  }

  /**
   * @param type response type ("response" or "event")
   * @param response from the V8 VM debugger
   */
  private void handleResponse(V8MessageType type, final JSONObject response) {
    String commandString = JsonUtil.getAsString(response, V8MessageType.RESPONSE == type
        ? V8Protocol.KEY_COMMAND
        : V8Protocol.KEY_EVENT);
    DebuggerCommand command = DebuggerCommand.forString(commandString);
    if (command == null) {
      LOGGER.log(Level.WARNING,
          MessageFormat.format("Unknown command in V8 debugger reply JSON: {0}", commandString));
      return;
    }
    final V8ResponseCallback handler = commandToHandlerMap.get(command);
    if (handler == null) {
      return;
    }
    handler.messageReceived(response);
  }

  private void checkNull(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }

  private void removeAllCallbacks() {
    for (Iterator<CallbackEntry> it = seqToV8Callbacks.values().iterator(); it.hasNext();) {
      CallbackEntry entry = it.next();
      if (entry.semaphore != null) {
        entry.semaphore.release();
      }
      it.remove();
    }
  }

  private void processClosed(JSONObject json) {
    synchronized (fieldAccessLock) {
      if (detachCallback != null) {
        // A detach request might be in flight. It should succeed in this case.
        notifyDetachCallback(Result.OK);
      }
    }
    context.getTab().getDebugEventListener().closed();
    onDebuggerDetached();
  }

  /**
   * This method is invoked from synchronized code sections.
   *
   * @param result to notify the callback with
   */
  private void notifyDetachCallback(Result result) {
    try {
      detachCallback.resultReceived(result);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Exception in the detach callback", e);
    } finally {
      detachCallback = null;
    }
  }

  private void processAttach(JSONObject json) {
    Long resultValue = JsonUtil.getAsLong(json, ChromeDevToolsProtocol.RESULT.key);
    Result result = Result.forCode(resultValue.intValue());
    // Message destination equals context.getTabId()
    synchronized (fieldAccessLock) {
      if (result != null && result == Result.OK) {
        isAttached = true;
      } else {
        if (result == null) {
          result = Result.DEBUGGER_ERROR;
        }
      }
      if (attachCallback != null) {
        try {
          attachCallback.resultReceived(result);
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Exception in the attach callback", e);
        } finally {
          attachCallback = null;
        }
      }
    }
  }

  private void processDetach(JSONObject json) {
    Long resultValue = JsonUtil.getAsLong(json, ChromeDevToolsProtocol.RESULT.key);
    Result result = Result.forCode(resultValue.intValue());
    if (result != null && result == Result.OK) {
      onDebuggerDetached();
    } else {
      if (result == null) {
        result = Result.DEBUGGER_ERROR;
      }
    }
    synchronized (fieldAccessLock) {
      if (detachCallback != null) {
        notifyDetachCallback(result);
      }
    }
  }

  private void processDebuggerCommand(JSONObject json) {
    final JSONObject v8Json = JsonUtil.getAsJSON(json, ChromeDevToolsProtocol.DATA.key);
    checkNull(v8Json, "'data' field not found");
    V8MessageType type = V8MessageType.forString(JsonUtil.getAsString(v8Json, V8Protocol.KEY_TYPE));
    handleResponse(type, v8Json);
    if (V8MessageType.RESPONSE == type) {
      final int requestSeq = JsonUtil.getAsLong(v8Json, V8Protocol.KEY_REQSEQ).intValue();
      checkNull(requestSeq, "Could not read 'request_seq' from debugger reply");
      final CallbackEntry callbackEntry = seqToV8Callbacks.remove(requestSeq);
      if (callbackEntry != null) {
        LOGGER.log(Level.INFO,
            MessageFormat.format(
                "Request-response roundtrip: {0}ms",
                getCurrentMillis() - callbackEntry.commitMillis));
        Thread t = new Thread(new Runnable() {
          public void run() {
            try {
              if (callbackEntry.v8HandlerCallback != null) {
                LOGGER.log(
                    Level.INFO, "Notified debugger command callback, request_seq=" + requestSeq);
                callbackEntry.v8HandlerCallback.messageReceived(v8Json);
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

  private void processNavigated(JSONObject json) {
    context.navigated(JsonUtil.getAsString(json, ChromeDevToolsProtocol.DATA.key));
  }

  private Connection getConnection() {
    return browserImpl.getConnection();
  }


  private static boolean isMessageContextDependent(DebuggerMessage message) {
    DebuggerCommand command = DebuggerCommand.forString(message.getCommand());
    if (command == null) {
      return false;
    }
    return CONTEXT_DEPENDENT_COMMANDS.contains(command);
  }

}
