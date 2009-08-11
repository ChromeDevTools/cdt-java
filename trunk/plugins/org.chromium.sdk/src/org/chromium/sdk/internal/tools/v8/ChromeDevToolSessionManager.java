// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.BrowserImpl;
import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.DebugSessionManager;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.MessageFactory;
import org.chromium.sdk.internal.Result;
import org.chromium.sdk.internal.tools.ChromeDevToolsProtocol;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Message;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  /** The debug context for this handler. */
  private final DebugContextImpl context;

  /** A synchronization object for the field access/modification. */
  private final Object fieldAccessLock = new Object();

  // The fields access is synchronized
  private boolean isAttached;

  private ResultAwareCallback attachCallback;

  private ResultAwareCallback detachCallback;

  private final V8CommandProcessor v8CommandProcessor;

  /**
   * A no-op JavaScript to evaluate.
   */
  public static final String JAVASCRIPT_VOID = "javascript:void(0);";

  public ChromeDevToolSessionManager(BrowserTabImpl browserTabImpl, DebugContextImpl context) {
    this.browserTabImpl = browserTabImpl;
    this.context = context;
    ChromeDevToolMessageOutput messageOutput = new ChromeDevToolMessageOutput(browserTabImpl);
    this.v8CommandProcessor = new V8CommandProcessor(messageOutput, context);
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
    v8CommandProcessor.removeAllCallbacks();
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


  public void sendV8Command(DebuggerMessage message,
      boolean isImmediate, V8CommandProcessor.V8HandlerCallback v8HandlerCallback) {
    v8CommandProcessor.sendV8Command(message, isImmediate, v8HandlerCallback);
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
    getConnection().send(MessageFactory.attach(String.valueOf(browserTabImpl.getId())));

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
      toolHandler.onDebuggerDetached();
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
    getConnection().send(MessageFactory.detach(String.valueOf(browserTabImpl.getId())));

    try {
      sem.tryAcquire(BrowserImpl.OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Fall through
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

    public void onDebuggerDetached() {
      onDebuggerDetachedImpl();
    }
  };

  private void processClosed(JSONObject json) {
    synchronized (fieldAccessLock) {
      if (detachCallback != null) {
        // A detach request might be in flight. It should succeed in this case.
        notifyDetachCallback(Result.OK);
      }
    }
    browserTabImpl.getTabDebugEventListener().closed();
    onDebuggerDetachedImpl();
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
      onDebuggerDetachedImpl();
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
    JSONObject v8Json = JsonUtil.getAsJSON(json, ChromeDevToolsProtocol.DATA.key);
    V8CommandProcessor.checkNull(v8Json, "'data' field not found");
    v8CommandProcessor.processIncomingJson(v8Json);
  }

  private void processNavigated(JSONObject json) {
    String newUrl = JsonUtil.getAsString(json, ChromeDevToolsProtocol.DATA.key);
    context.navigated();
    getTabDebugEventListener().navigated(newUrl);
  }

  private Connection getConnection() {
    return browserTabImpl.getBrowser().getConnection();
  }

  public V8CommandProcessor getV8CommandProcessor() {
    return v8CommandProcessor;
  }

  private class ChromeDevToolMessageOutput implements V8CommandOutput {
    private final String destination;

    ChromeDevToolMessageOutput(BrowserTabImpl browserTabImpl) {
      this.destination = String.valueOf(browserTabImpl.getId());
    }

    public void send(DebuggerMessage debuggerMessage, boolean isImmediate) {
      getConnection().send(
          MessageFactory.debuggerCommand(
              destination,
              JsonUtil.streamAwareToJson(debuggerMessage)));
      if (isImmediate) {
        getConnection().send(
            MessageFactory.evaluateJavascript(destination, JAVASCRIPT_VOID));
      }
    }
  }
}
