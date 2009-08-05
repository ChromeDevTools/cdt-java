// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.json.simple.JSONObject;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of
 * {@link V8CommandProcessor#sendV8CommandBlocking(
 * DebuggerMessage, V8CommandProcessor.V8HandlerCallback)}
 * which blocks until the response has been received and callback (if any) has
 * been invoked.
 */
class BlockingV8RequestCommand implements Runnable {
  /**
   * The waiting time for an operation. An operation is considered timed out if
   * it does not complete within this period.
   */
  private static final long OPERATION_TIMEOUT_MS = 120000;

  /**
   * The handler to use while sending a message.
   */
  private final V8CommandProcessor v8CommandProcessor;

  /**
   * The message to send.
   */
  private final DebuggerMessage message;

  /**
   * The callback to invoke once the result is available.
   */
  private final V8CommandProcessor.V8HandlerCallback v8HandlerCallback;

  /**
   * Whether to send the "evaluate_javascript" command so that the primary
   * command can be executed immediately while NOT on a breakpoint.
   */
  private final boolean isImmediate;

  private Exception exception = null;

  /**
   * @param v8CommandProcessor the V8CommandProcessor instance
   * @param message to send
   * @param immediate whether to evaluate JavaScript so that the request is
   *        handled immediately (should always be the case if potentially
   *        calling a command not on a breakpoint)
   * @param v8HandlerCallback to invoke on reply from the browser
   */
  BlockingV8RequestCommand(V8CommandProcessor v8CommandProcessor, DebuggerMessage message,
      boolean immediate, V8CommandProcessor.V8HandlerCallback v8HandlerCallback) {
    this.v8CommandProcessor = v8CommandProcessor;
    this.message = message;
    this.v8HandlerCallback = v8HandlerCallback;
    this.isImmediate = immediate;
  }

  public void run() {
    Exception[] result = new Exception[1];
    try {
      final Semaphore sem = new Semaphore(0);
      V8CommandProcessor.V8HandlerCallback commandCallback =
          new V8CommandProcessor.V8HandlerCallback() {
        public void messageReceived(JSONObject response) {
          try {
            if (v8HandlerCallback != null) {
              v8HandlerCallback.messageReceived(response);
            }
          } finally {
            sem.release();
          }
        }

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
      v8CommandProcessor.sendV8Command(message, isImmediate, commandCallback);
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
