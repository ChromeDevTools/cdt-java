// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8;

import org.json.simple.JSONObject;

/**
 * An abstract base implementation of reply handlers for certain V8 command.
 */
public abstract class V8ReplyHandler {

  private final V8DebuggerToolHandler toolHandler;

  public V8ReplyHandler(V8DebuggerToolHandler toolHandler) {
    this.toolHandler = toolHandler;
  }

  protected V8DebuggerToolHandler getToolHandler() {
    return toolHandler;
  }

  /**
   * Handles the reply for the V8 command(s) the handler has been registered
   * for.
   *
   * @param reply from the Chromium V8 VM
   */
  public abstract void handleReply(JSONObject reply);
}
