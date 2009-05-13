// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.processor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.JsLineBreakpoint;
import org.chromium.debug.core.tools.v8.BreakpointType;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.chromium.debug.core.tools.v8.V8ReplyHandler;
import org.chromium.debug.core.tools.v8.model.mirror.Script;
import org.chromium.debug.core.tools.v8.request.ScriptsRequestMessage;
import org.chromium.debug.core.tools.v8.request.V8Request;
import org.chromium.debug.core.util.JsonUtil;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the breakpoint-related V8 command replies and events.
 */
public class BreakpointProcessor extends V8ReplyHandler implements IBreakpointListener {

  public BreakpointProcessor(V8DebuggerToolHandler debuggerToolHandler) {
    super(debuggerToolHandler);
  }

  @Override
  public void handleReply(JSONObject reply) {
    String type = JsonUtil.getAsString(reply, Protocol.KEY_TYPE);
    if (Protocol.TYPE_EVENT.equals(type)) {
      String event = JsonUtil.getAsString(reply, Protocol.KEY_EVENT);
      if (event.equals(Protocol.EVENT_BREAK)) {
        onBreakpointsHit(reply);
        try {
          getToolHandler().sendV8Command(V8Request.backtrace(null, null).getMessage(), null);
          getToolHandler().sendV8Command(
              V8Request.scripts(ScriptsRequestMessage.SCRIPTS_NORMAL, true).getMessage(), null);
          getToolHandler().sendV8Command(V8Request.frame(0).getMessage(), null);
        } catch (IOException e) {
          ChromiumDebugPlugin.log(e);
        }
      }
    }
  }

  private void onBreakpointsHit(JSONObject reply) {
    JSONObject body = JsonUtil.getAsJSON(reply, Protocol.BREAK_BODY);
    JSONArray breakpointIdsArray = JsonUtil.getAsJSONArray(body, Protocol.BREAK_BREAKPOINTS);
    if (breakpointIdsArray == null) {
      return;
    }
    Set<Long> breakpointIds = new HashSet<Long>(5);
    for (int i = 0, size = breakpointIdsArray.size(); i < size; ++i) {
      breakpointIds.add((Long) breakpointIdsArray.get(i));
    }
    IBreakpoint[] breakpoints =
        DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
            ChromiumDebugPlugin.DEBUG_MODEL_ID);
    for (IBreakpoint breakpoint : breakpoints) {
      JsLineBreakpoint jsBreakpoint = (JsLineBreakpoint) breakpoint;
      if (breakpointIds.contains(Long.valueOf(jsBreakpoint.getId()))) {
        jsBreakpoint.setIgnoreCount(null); // reset ignore count as we've hit it
      }
    }
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    if (!getToolHandler().getDebugTarget().supportsBreakpoint(breakpoint)) {
      return;
    }
    try {
      if (breakpoint.isEnabled()) {
        // Class cast is ensured by the supportsBreakpoint implementation
        final JsLineBreakpoint lineBreakpoint = (JsLineBreakpoint) breakpoint;
        String resourceName = breakpoint.getMarker().getResource().getName();
        Script script =
            getToolHandler().getExecution().getScriptManager()
                .findScriptFromResourceName(resourceName);
        getToolHandler().sendV8Command(
            V8Request.setBreakpoint(BreakpointType.SCRIPT,
                script.getName(),
                // ILineBreakpoint lines are 1-based while V8 lines are 0-based
                (lineBreakpoint.getLineNumber() - 1) + script.getLineOffset(),
                null, breakpoint.isEnabled(), lineBreakpoint.getCondition(),
                lineBreakpoint.getIgnoreCount()).getMessage(),
            new V8DebuggerToolHandler.MessageReplyCallback() {
              @Override
              public void replyReceived(JSONObject reply) {
                // TODO(apavlov): handle multiple destinations here if needed
                JSONObject body = JsonUtil.getAsJSON(reply, Protocol.BODY_SETBP);
                int bpNum = JsonUtil.getAsLong(body, Protocol.BODY_BREAKPOINT).intValue();
                lineBreakpoint.setId(bpNum);
              }
            });
        getToolHandler().sendEvaluateJavascript();
      }
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    } catch (IOException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (!getToolHandler().getDebugTarget().supportsBreakpoint(breakpoint)) {
      return;
    }
    try {
      getToolHandler().sendV8Command(
          V8Request.changeBreakpoint((JsLineBreakpoint) breakpoint).getMessage(), null);
    } catch (IOException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (!getToolHandler().getDebugTarget().supportsBreakpoint(breakpoint)) {
      return;
    }
    try {
      if (breakpoint.isEnabled()) {
        // Class cast is ensured by the supportsBreakpoint implementation
        final JsLineBreakpoint lineBreakpoint = (JsLineBreakpoint) breakpoint;
        getToolHandler().sendV8Command(
            V8Request.clearBreakpoint(lineBreakpoint).getMessage(), null);
      }
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    } catch (IOException e) {
      ChromiumDebugPlugin.log(e);
    }
  }
}
