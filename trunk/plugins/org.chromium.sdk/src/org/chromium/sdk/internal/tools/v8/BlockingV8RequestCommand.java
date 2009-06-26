// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.json.simple.JSONObject;

/**
 * An implementation of
 * {@link V8DebuggerToolHandler#sendV8CommandBlocking(
 * DebuggerMessage, boolean, org.chromium.sdk.internal.BrowserTabImpl.V8HandlerCallback)}
 * which blocks until the response has been received and callback (if any) has
 * been invoked.
 */
class BlockingV8RequestCommand implements Runnable {

  /**
   * The waiting time for an operation. An operation is considered timed out if
   * it does not complete within this period.
   */
  private static final long OPERATION_TIMEOUT_MS = 30000;

  /**
   * The handler to use while sending a message.
   */
  private final V8DebuggerToolHandler handler;

  /**
   * The message to send.
   */
  private final DebuggerMessage message;

  /**
   * The callback to invoke once the result is available.
   */
  private final BrowserTabImpl.V8HandlerCallback v8HandlerCallback;

  /**
   * Whether to send the "evaluate_javascript" command so that the primary
   * command can be executed immediately while NOT on a breakpoint.
   */
  private final boolean isImmediate;

  private Exception exception = null;

  /**
   * @param handler the V8DebuggerToolHandler instance
   * @param message to send
   * @param immediate whether to evaluate JavaScript so that the request is
   *        handled immediately (should always be the case if potentially
   *        calling a command not on a breakpoint)
   * @param v8HandlerCallback to invoke on reply from the browser
   */
  BlockingV8RequestCommand(V8DebuggerToolHandler handler, DebuggerMessage message,
      boolean immediate, BrowserTabImpl.V8HandlerCallback v8HandlerCallback) {
    this.handler = handler;
    this.message = message;
    this.v8HandlerCallback = v8HandlerCallback;
    this.isImmediate = immediate;
  }

  @Override
  public void run() {
    Exception[] result = new Exception[1];
    try {
      final Semaphore sem = new Semaphore(0);
      BrowserTabImpl.V8HandlerCallback commandCallback = new BrowserTabImpl.V8HandlerCallback() {
        public void messageReceived(JSONObject response) {
          try {
            if (v8HandlerCallback != null) {
              v8HandlerCallback.messageReceived(response);
            }
          } finally {
            sem.release();
          }
        }

        @Override
        public void failure(String message) {
          try {
            if (v8HandlerCallback != null) {
              v8HandlerCallback.failure(message);
            }
          } finally {
            sem.release();
          }
        }
      };
      handler.sendV8Command(message, commandCallback);
      if (isImmediate) {
        handler.sendEvaluateJavascript("javascript:void(0);");
      }
      if (!sem.tryAcquire(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
        commandCallback.failure("Timed out");
      }
    } catch (Exception e) {
      result[0] = e;
    }
    exception = result[0];
  }

  /**
   * @return the exception which happened during the execution of the command,
   *         or null if none
   */
  Exception getException() {
    return exception;
  }
}
