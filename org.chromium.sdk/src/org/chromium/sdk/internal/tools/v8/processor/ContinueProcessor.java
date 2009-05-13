// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.processor;

import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.chromium.debug.core.tools.v8.V8ReplyHandler;
import org.chromium.debug.core.util.JsonUtil;
import org.json.simple.JSONObject;

/**
 * Handles the "continue" V8 command replies.
 */
public class ContinueProcessor extends V8ReplyHandler {

  public ContinueProcessor(V8DebuggerToolHandler toolHandler) {
    super(toolHandler);
  }

  @Override
  public void handleReply(JSONObject reply) {
    if (JsonUtil.getAsBoolean(reply, Protocol.BODY_RUNNING)) {
      getToolHandler().resumed();
    }
  }

}
