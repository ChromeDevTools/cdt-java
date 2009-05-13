// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.JsThread;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.tools.ToolHandler;
import org.chromium.debug.core.tools.v8.model.mirror.Execution;
import org.chromium.debug.core.tools.v8.processor.BacktraceFrameProcessor;
import org.chromium.debug.core.tools.v8.processor.BreakpointProcessor;
import org.chromium.debug.core.tools.v8.processor.ContinueProcessor;
import org.chromium.debug.core.tools.v8.processor.ScriptsProcessor;
import org.chromium.debug.core.tools.v8.request.ScriptsRequestMessage;
import org.chromium.debug.core.tools.v8.request.V8DebugRequestMessage;
import org.chromium.debug.core.tools.v8.request.V8Request;
import org.chromium.debug.core.transport.Message;
import org.chromium.debug.core.transport.MessageFactory;
import org.chromium.debug.core.util.JsonUtil;
import org.chromium.debug.core.util.LoggingUtil;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.osgi.util.NLS;
import org.json.simple.JSONObject;

/**
 * Handles the interaction with the "V8Debugger" tool.
 */
public class V8DebuggerToolHandler extends ToolHandler {

  /**
   * The interface to run callbacks in response to V8 debugger replies.
   */
  public interface MessageReplyCallback {
    /**
     * This method is invoked whenever a reply to a V8 message is received.
     * @param reply the actual reply body
     */
    void replyReceived(JSONObject reply);
  }

  private static class CallbackEntry {
    MessageReplyCallback callback;

    long commitMillis;

    Semaphore semaphore;

    public CallbackEntry(MessageReplyCallback callback, long commitMillis) {
      super();
      this.callback = callback;
      this.commitMillis = commitMillis;
    }

    public CallbackEntry(MessageReplyCallback callback, long commitMillis,
        Semaphore semaphore) {
      this(callback, commitMillis);
      this.semaphore = semaphore;
    }
  }

  public static final int TAB_DETACHED = -1;

  private final Map<Integer, CallbackEntry> seqToV8Callbacks =
      new HashMap<Integer, CallbackEntry>();

  private final JsThread thread;

  private final Execution execution;

  private final Map<V8Command, V8ReplyHandler> commandToHandlerMap =
      new HashMap<V8Command, V8ReplyHandler>();

  private final BreakpointProcessor bpp;

  private volatile int currentTab = -1;

  private volatile boolean isAttached;

  private StackFrame[] frames;

  public V8DebuggerToolHandler(Execution execution) {
    super(execution.getDebugTarget());
    this.execution = execution;
    this.thread = new JsThread(this);

    commandToHandlerMap.put(V8Command.SCRIPTS, new ScriptsProcessor(this));

    bpp = new BreakpointProcessor(this);
    commandToHandlerMap.put(V8Command.CHANGEBREAKPOINT, bpp);
    commandToHandlerMap.put(V8Command.SETBREAKPOINT, bpp);
    commandToHandlerMap.put(V8Command.CLEARBREAKPOINT, bpp);
    commandToHandlerMap.put(V8Command.BREAK /* event */, bpp);
    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(bpp);

    BacktraceFrameProcessor bfp = new BacktraceFrameProcessor(this);
    commandToHandlerMap.put(V8Command.BACKTRACE, bfp);
    commandToHandlerMap.put(V8Command.FRAME, bfp);

    commandToHandlerMap.put(V8Command.CONTINUE, new ContinueProcessor(this));
  }

  @Override
  public void handleMessage(final Message message)
      throws IllegalArgumentException {
    JSONObject json = JsonUtil.jsonObjectFromJson(message.getContent());
    String command = JsonUtil.getAsString(json, Protocol.KEY_COMMAND);
    if (V8DebuggerToolCommand.ATTACH.commandName.equals(command)) {
      Long result = JsonUtil.getAsLong(json, Protocol.KEY_RESULT);
      if (result != null && result == 0) {
        currentTab = Long.valueOf(message.getDestination()).intValue();
        isAttached = true;

        try {
          sendV8Command(
              V8Request.scripts(ScriptsRequestMessage.SCRIPTS_NORMAL, true).getMessage(), null);
          sendEvaluateJavascript();
        } catch (IOException e) {
          ChromiumDebugPlugin.log(e);
          return;
        }
        getDebugTarget().fireCreationEvent();
        resumed();
      } else {
        ChromiumDebugPlugin.logError(
            "Could not attach to tab {0}, result={1}", //$NON-NLS-1$
            currentTab, result);
        return;
      }
    } else if (V8DebuggerToolCommand.DETACH.commandName.equals(command)) {
      Long result = JsonUtil.getAsLong(json, Protocol.KEY_RESULT);
      if (result != null && result == 0) {
        onDetachAcknowledge();
      } else {
        ChromiumDebugPlugin.logError(
            "Could not detach from tab {0}, result={1}", //$NON-NLS-1$
            currentTab, result);
        return;
      }
    } else if (V8DebuggerToolCommand.DEBUGGER_COMMAND.commandName.equals(command)) {
      final JSONObject v8Json = JsonUtil.getAsJSON(json, Protocol.KEY_DATA);
      checkNull(v8Json, "'data' field not found"); //$NON-NLS-1$
      String type = JsonUtil.getAsString(v8Json, Protocol.KEY_TYPE);
      if (Protocol.TYPE_RESPONSE.equals(type)) {
        int requestSeq = JsonUtil.getAsLong(v8Json, Protocol.KEY_REQSEQ).intValue();
        checkNull(requestSeq, "Could not read 'request_seq' from debugger reply"); //$NON-NLS-1$
        final CallbackEntry callbackEntry = seqToV8Callbacks.remove(requestSeq);
        if (callbackEntry != null) {
          LoggingUtil.logV8DebuggerTool("Request-response roundtrip: {0}ms", //$NON-NLS-1$
              (System.currentTimeMillis() - callbackEntry.commitMillis));
          Thread t = new Thread(new Runnable() {
            public void run() {
              if (callbackEntry.callback != null) {
                callbackEntry.callback.replyReceived(v8Json);
              }
              // Note that the semaphore is released ASYNCHRONOUSLY with regard
              // to the handleResponse call.
              if (callbackEntry.semaphore != null) {
                callbackEntry.semaphore.release();
              }
            }
          });
          t.start();
        }
      }
      handleResponse(type, v8Json);
    }
  }

  private void onDetachAcknowledge() {
    getDebugTarget().getSocketConnection().shutdown(false);
    onDebuggerDetached();
  }

  private void checkNull(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }

  @Override
  public void onDebuggerDetached() {
    removeAllCallbacks();
    DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(
        bpp);
    if (isAttached) {
      isAttached = false;
      currentTab = -1;
    }
    getDebugTarget().fireTerminateEvent();
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

  public int getAttachedTab() {
    if (!isAttached) {
      throw new IllegalStateException("Debugger not attached to any tab"); //$NON-NLS-1$
    }
    return currentTab;
  }

  public boolean isAttached() {
    return isAttached;
  }

  public void sendEvaluateJavascript() throws IOException {
    getDebugTarget().getSocketConnection().send(
        MessageFactory.getInstance().evaluateJavascript(
            String.valueOf(getAttachedTab()), "javascript:void(0);")); //$NON-NLS-1$
  }

  public void sendV8Command(V8DebugRequestMessage message,
      MessageReplyCallback callback) throws IOException {
    if (callback != null) {
      seqToV8Callbacks.put(message.getSeq(),
          new CallbackEntry(callback, System.currentTimeMillis()));
    }
    try {
      getDebugTarget().getSocketConnection().sendToDebugger(
          String.valueOf(getAttachedTab()), message);
    } catch (RuntimeException e) {
      if (callback != null) {
        seqToV8Callbacks.remove(message.getSeq());
      }
      throw e;
    } catch (IOException e) {
      if (callback != null) {
        seqToV8Callbacks.remove(message.getSeq());
      }
      throw e;
    }
  }

  /**
   * Sends a command to the V8 debugger but blocks until a response has been
   * received and the client-supplied callback (if any) invoked.
   */
  public void sendV8CommandBlocking(V8DebugRequestMessage message,
      MessageReplyCallback callback) throws IOException {
    Semaphore semaphore = new Semaphore(0);
    seqToV8Callbacks.put(message.getSeq(),
        new CallbackEntry(callback, System.currentTimeMillis(), semaphore));
    try {
      getDebugTarget().getSocketConnection().sendToDebugger(
          String.valueOf(getAttachedTab()), message);
      semaphore.acquire();
    } catch (RuntimeException e) {
      removeCallbackIfPut(message.getSeq());
      throw e;
    } catch (IOException e) {
      removeCallbackIfPut(message.getSeq());
      throw e;
    } catch (InterruptedException e) {
      removeCallbackIfPut(message.getSeq());
      throw new IOException(e);
    }
  }

  private void removeCallbackIfPut(Integer seq) {
    seqToV8Callbacks.remove(seq);
  }

  /**
   * @param type
   *          response type ("response" or "event")
   * @param v8Response
   *          response from the V8 VM debugger
   */
  private void handleResponse(String type, JSONObject v8Response) {
    String commandString = JsonUtil.getAsString(v8Response,
        Protocol.TYPE_RESPONSE.equals(type)
            ? Protocol.KEY_COMMAND
            : Protocol.KEY_EVENT);
    V8Command command = V8Command.forString(commandString);
    if (command == null) {
      LoggingUtil.logV8DebuggerTool(
          "Bad command in V8 debugger reply JSON: {0}", //$NON-NLS-1$
          commandString);
      return;
    }
    V8ReplyHandler handler = commandToHandlerMap.get(command);
    if (handler == null) {
      LoggingUtil.logV8DebuggerTool("Unregistered handler for V8 command: {0}", //$NON-NLS-1$
          commandString);
      return;
    }
    handler.handleReply(v8Response);
  }

  /**
   * Attaches the remote debugger to the specified Chromium tab.
   *
   * @param targetTab
   *          the target tab id
   */
  public void attachToTab(int targetTab) {
    if (isAttached()) {
      throw new IllegalStateException(NLS.bind(
          "Already attached to the tab {0}", //$NON-NLS-1$
          currentTab));
    }
    currentTab = targetTab;
    getDebugTarget().getSocketConnection().send(
        MessageFactory.getInstance().attach(
            String.valueOf(targetTab)));
  }

  public void detachFromTab() {
    if (!isAttached()) {
      onDetachAcknowledge();
      return;
    }
    getDebugTarget().getSocketConnection().send(
        MessageFactory.getInstance().detach(String.valueOf(currentTab)));
  }

  public JsThread getThread() {
    return thread;
  }

  /**
   * @return current set of stack frames associated with their scripts
   */
  public StackFrame[] getStackFrames() {
    if (frames == null) {
      int frameCount = execution.getFrameCount();
      frames = new StackFrame[frameCount];
      for (int i = 0; i < frameCount; ++i) {
        frames[i] = new StackFrame(this, execution.getFrame(i), i);
        getExecution().hookupScriptToFrame(i);
      }
    }
    return frames;
  }

  public Execution getExecution() {
    return execution;
  }

  public void step(StepAction action, MessageReplyCallback callback)
      throws IOException {
    requestContinue(action, null, callback);
  }

  public void resumed() {
    frames = null;
    getDebugTarget().resumed(DebugEvent.CLIENT_REQUEST);
  }

  public void resumeRequested() throws IOException {
    requestContinue(null, null, null);
  }

  private void requestContinue(StepAction stepAction, Integer stepCount,
      MessageReplyCallback callback) throws IOException {
    sendV8Command(V8Request.goOn(stepAction, stepCount).getMessage(), callback);
  }

}
