// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext.State;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.DebugContextImpl.SendingType;
import org.chromium.sdk.internal.tools.v8.BreakpointManager;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Handles the suspension-related V8 command replies and events.
 */
public class BreakpointProcessor extends V8ResponseCallback {

  public BreakpointProcessor(DebugContextImpl context) {
    super(context);
  }

  public void messageReceived(JSONObject response) {
    V8MessageType type =
        V8MessageType.forString(JsonUtil.getAsString(response, V8Protocol.KEY_TYPE));
    if (V8MessageType.EVENT == type) {
      String event = JsonUtil.getAsString(response, V8Protocol.KEY_EVENT);
      DebugContextImpl debugContext = getDebugContext();
      if (V8Protocol.EVENT_BREAK.key.equals(event)) {
        debugContext.setState(State.NORMAL);
        onBreakpointsHit(response);
      } else if (V8Protocol.EVENT_EXCEPTION.key.equals(event)) {
        debugContext.setState(State.NORMAL);
        debugContext.getBreakpointManager().onBreakpointsHit(Collections.<Breakpoint> emptySet());
        debugContext.setException(response);
      }
      debugContext.sendMessage(
          SendingType.ASYNC,
          DebuggerMessageFactory.backtrace(null, null, true, getDebugContext().getToken()),
          null);
    }
  }

  private void onBreakpointsHit(JSONObject reply) {
    JSONObject body = JsonUtil.getAsJSON(reply, V8Protocol.BREAK_BODY);
    JSONArray breakpointIdsArray = JsonUtil.getAsJSONArray(body, V8Protocol.BREAK_BREAKPOINTS);
    BreakpointManager breakpointManager = getDebugContext().getBreakpointManager();
    if (breakpointIdsArray == null) {
      // Suspended on step end.
      breakpointManager.onBreakpointsHit(Collections.<Breakpoint> emptySet());
      return;
    }
    Collection<Breakpoint> breakpointsHit = new ArrayList<Breakpoint>(breakpointIdsArray.size());
    for (int i = 0, size = breakpointIdsArray.size(); i < size; ++i) {
      Breakpoint existingBp = breakpointManager.getBreakpoint((Long)breakpointIdsArray.get(i));
      if (existingBp != null) {
        breakpointsHit.add(existingBp);
      }
    }
    breakpointManager.onBreakpointsHit(breakpointsHit);
  }

}
