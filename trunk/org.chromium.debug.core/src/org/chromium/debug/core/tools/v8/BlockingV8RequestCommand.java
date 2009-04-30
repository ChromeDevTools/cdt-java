// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8;

import java.util.concurrent.Semaphore;

import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler.MessageReplyCallback;
import org.chromium.debug.core.tools.v8.request.V8Request;
import org.json.simple.JSONObject;

/**
 * A V8 request command which blocks until the response has been received and
 * callback (if any) has been invoked.
 */
public class BlockingV8RequestCommand implements Runnable {

  private final V8DebuggerToolHandler handler;

  private final V8Request request;

  private final MessageReplyCallback callback;

  private Exception exception = null;

  /**
   * @param handler the V8DebuggerToolHandler instance
   * @param request to send
   * @param callback to invoke on reply from the Chromium V8 VM
   */
  public BlockingV8RequestCommand(V8DebuggerToolHandler handler,
      V8Request request, MessageReplyCallback callback) {
    this.handler = handler;
    this.request = request;
    this.callback = callback;
  }

  @Override
  public void run() {
    Exception[] result = new Exception[1];
    try {
      final Semaphore sem = new Semaphore(0);
      handler.sendV8Command(request.getMessage(), new MessageReplyCallback() {
        public void replyReceived(JSONObject reply) {
          if (callback != null) {
            callback.replyReceived(reply);
          }
          sem.release();
        }
      });
      sem.acquire();
    } catch (Exception e) {
      result[0] = e;
    }
    exception = result[0];
  }

  /**
   * @return the exception which happened during the execution of the command,
   *         or null if none
   */
  public Exception getException() {
    return exception;
  }
}
