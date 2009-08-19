// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONObject;

/**
 * Handles the "backtrace" V8 command replies.
 */
public class BacktraceProcessor extends V8ResponseCallback {

  public BacktraceProcessor(DebugContextImpl context) {
    super(context);
  }

  @Override
  public void messageReceived(final JSONObject response) {
    V8MessageType type = V8MessageType.forString(
        JsonUtil.getAsString(response, V8Protocol.KEY_TYPE));
    String commandString = JsonUtil.getAsString(response, V8MessageType.RESPONSE == type
        ? V8Protocol.KEY_COMMAND
        : V8Protocol.KEY_EVENT);
    DebuggerCommand command = DebuggerCommand.forString(commandString);

    switch (command) {
      case BACKTRACE: {
        Thread t = new Thread(new Runnable() {
          public void run() {
            getDebugContext().setFrames(response);
            suspend();
          }
        });
        t.setDaemon(true);
        t.start();
        break;
      }
    }
  }

  protected void suspend() {
    getDebugContext().getDebugEventListener().suspended(getDebugContext());
  }

}
