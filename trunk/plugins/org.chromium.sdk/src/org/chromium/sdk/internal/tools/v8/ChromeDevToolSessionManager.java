// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.BrowserImpl;
import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.DebugSessionManager;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.Result;
import org.chromium.sdk.internal.tools.ChromeDevToolsProtocol;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.tools.ToolOutput;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.transport.Message;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Handles the interaction with the "V8Debugger" tool.
 */
public class ChromeDevToolSessionManager implements DebugSessionManager {

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

  /** The class logger. */
  private static final Logger LOGGER =
      Logger.getLogger(ChromeDevToolSessionManager.class.getName());

  /** The host BrowserTabImpl instance. */
  private final BrowserTabImpl browserTabImpl;

  private final ToolOutput toolOutput;

  /** The debug context for this handler. */
  private final DebugSession debugSession;

  /** A synchronization object for the field access/modification. */
  private final Object fieldAccessLock = new Object();

  // The fields access is synchronized
  private boolean isAttached;

  private final AtomicReference<ResultAwareCallback> attachCallback =
      new AtomicReference<ResultAwareCallback>(null);

  private final AtomicReference<ResultAwareCallback> detachCallback =
      new AtomicReference<ResultAwareCallback>(null);

  /**
   * A no-op JavaScript to evaluate.
   */
  public static final String JAVASCRIPT_VOID = "javascript:void(0);";

  public ChromeDevToolSessionManager(BrowserTabImpl browserTabImpl, ToolOutput toolOutput, DebugSession debugSession) {
    this.browserTabImpl = browserTabImpl;
    this.toolOutput = toolOutput;
    this.debugSession = debugSession;
  }

  public DebugEventListener getDebugEventListener() {
    return browserTabImpl.getDebugEventListener();
  }

  private TabDebugEventListener getTabDebugEventListener() {
    return browserTabImpl.getTabDebugEventListener();
  }

  private void handleChromeDevToolMessage(final Message message) {
    JSONObject json;
    try {
      json = JsonUtil.jsonObjectFromJson(message.getContent());
    } catch (ParseException e) {
      LOGGER.log(Level.SEVERE, "Invalid JSON received: {0}", message.getContent());
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
    onDebuggerDetachedImpl();
  }

  private void onDebuggerDetachedImpl() {
    if (!isAttached()) {
      // We've already been notified.
      return;
    }
    synchronized (fieldAccessLock) {
      isAttached = false;
    }
    debugSession.getV8CommandProcessor().removeAllCallbacks();
    DebugEventListener debugEventListener = getDebugEventListener();
    if (debugEventListener != null) {
      debugEventListener.disconnected();
    }
    browserTabImpl.sessionTerminated();
  }

  public int getAttachedTab() {
    if (!isAttached()) {
      throw new IllegalStateException("Debugger is not attached to any tab");
    }
    return browserTabImpl.getId();
  }

  /**
   * @return whether the handler is attached to a tab
   */
  public boolean isAttached() {
    synchronized (fieldAccessLock) {
      return isAttached;
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

    String command = V8DebuggerToolMessageFactory.attach();
    return sendSimpleCommandSync(attachCallback, command);
  }

  /**
   * Detaches the remote debugger from the associated browser tab.
   *
   * @return the detachment result
   */
  public Result detachFromTab() {
    if (!isAttached()) {
      toolHandler.onDebuggerDetached();
      return Result.ILLEGAL_TAB_STATE;
    }

    String command = V8DebuggerToolMessageFactory.detach();
    Result result;
    try {
      result = sendSimpleCommandSync(detachCallback, command);
    } catch (AttachmentFailureException e) {
      result = null;
    }
    return result;
  }

  private Result sendSimpleCommandSync(AtomicReference<ResultAwareCallback> callbackReference, String command) throws AttachmentFailureException {
    final Semaphore sem = new Semaphore(0);
    final Result[] output = new Result[1];
    ResultAwareCallback callback = new ResultAwareCallback() {
      public void resultReceived(Result result) {
        output[0] = result;
        sem.release();
      }
    };

    boolean res = callbackReference.compareAndSet(null, callback);
    if (!res) {
      throw new IllegalStateException("Callback is already set");
    }

    boolean completed;
    try {
      toolOutput.send(command);

      try {
        completed = sem.tryAcquire(BrowserImpl.OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } finally {
      // Make sure we do not leave our callback behind us.
      callbackReference.compareAndSet(callback, null);
    }

    // If the command fails, notify the caller.
    if (!completed) {
      throw new AttachmentFailureException("Timed out", null);
    }

    return output[0];
  }

  public ToolHandler getToolHandler() {
    return toolHandler;
  }

  private final ToolHandler toolHandler = new ToolHandler() {
    public void handleMessage(Message message) {
      handleChromeDevToolMessage(message);
    }

    public void handleEos() {
      debugSession.getV8CommandProcessor().processEos();
    }

    public void onDebuggerDetached() {
      onDebuggerDetachedImpl();
    }
  };

  private void processClosed(JSONObject json) {
    synchronized (fieldAccessLock) {
      if (detachCallback != null) {
        // A detach request might be in flight. It should succeed in this case.
        notifyCallback(detachCallback, Result.OK);
      }
    }
    browserTabImpl.getTabDebugEventListener().closed();
    onDebuggerDetachedImpl();
  }

  /**
   * This method is invoked from synchronized code sections. It checks if there is a callback
   * provided in {@code callbackReference}. Sets callback to null.
   *
   * @param callbackReference reference which may hold callback
   * @param result to notify the callback with
   */
  private void notifyCallback(AtomicReference<ResultAwareCallback> callbackReference, Result result) {
    ResultAwareCallback callback = callbackReference.getAndSet(null);
    if (callback != null) {
      try {
        callback.resultReceived(result);
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Exception in the callback", e);
      }
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
    }
    notifyCallback(attachCallback, result);
  }

  private void processDetach(JSONObject json) {
    Long resultValue = JsonUtil.getAsLong(json, ChromeDevToolsProtocol.RESULT.key);
    Result result = Result.forCode(resultValue.intValue());
    if (result != null && result == Result.OK) {
      onDebuggerDetachedImpl();
    } else {
      if (result == null) {
        result = Result.DEBUGGER_ERROR;
      }
    }
    notifyCallback(detachCallback, result);
  }

  private void processDebuggerCommand(JSONObject json) {
    JSONObject v8Json = JsonUtil.getAsJSON(json, ChromeDevToolsProtocol.DATA.key);
    V8CommandProcessor.checkNull(v8Json, "'data' field not found");
    debugSession.getV8CommandProcessor().processIncomingJson(v8Json);
  }

  private void processNavigated(JSONObject json) {
    String newUrl = JsonUtil.getAsString(json, ChromeDevToolsProtocol.DATA.key);
    debugSession.navigated();
    getTabDebugEventListener().navigated(newUrl);
  }

  public static class V8CommandOutputImpl implements V8CommandOutput {
    private final ToolOutput toolOutput;

    public V8CommandOutputImpl(ToolOutput toolOutput) {
      this.toolOutput = toolOutput;
    }

    public void send(DebuggerMessage debuggerMessage, boolean isImmediate) {
      toolOutput.send(
          V8DebuggerToolMessageFactory.debuggerCommand(
              JsonUtil.streamAwareToJson(debuggerMessage)));
      if (isImmediate) {
        toolOutput.send(
            V8DebuggerToolMessageFactory.evaluateJavascript(JAVASCRIPT_VOID));
      }
    }
  }

  private static class V8DebuggerToolMessageFactory {

    static String attach() {
      return createDebuggerMessage(DebuggerToolCommand.ATTACH, null);
    }

    static String detach() {
      return createDebuggerMessage(DebuggerToolCommand.DETACH, null);
    }

    public static String debuggerCommand(String json) {
      return createDebuggerMessage(DebuggerToolCommand.DEBUGGER_COMMAND, json);
    }

    public static String evaluateJavascript(String javascript) {
      return createDebuggerMessage(DebuggerToolCommand.EVALUATE_JAVASCRIPT,
          JsonUtil.quoteString(javascript));
    }

    private static String createDebuggerMessage(
        DebuggerToolCommand command, String dataField) {
      StringBuilder sb = new StringBuilder("{\"command\":\"");
      sb.append(command.commandName).append('"');
      if (dataField != null) {
        sb.append(",\"data\":").append(dataField);
      }
      sb.append('}');
      return sb.toString();
    }
  }
}
