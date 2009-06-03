// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.BrowserTab.BreakpointCallback;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.tools.v8.BreakpointImpl;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.ScriptsMessage;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the suspension-related V8 command replies and events.
 */
public class BreakpointProcessor extends V8ResponseCallback {

  /**
   * This map shall contain only breakpoints with valid IDs.
   */
  private final Map<Integer, Breakpoint> idToBreakpoint = new HashMap<Integer, Breakpoint>();

  public BreakpointProcessor(DebugContextImpl context) {
    super(context);
  }

  @Override
  public void messageReceived(JSONObject response) {
    V8MessageType type =
        V8MessageType.forString(JsonUtil.getAsString(response, V8Protocol.KEY_TYPE));
    if (V8MessageType.EVENT == type) {
      String event = JsonUtil.getAsString(response, V8Protocol.KEY_EVENT);
      if (V8Protocol.EVENT_BREAK.key.equals(event)) {
        onBreakpointsHit(response);
        V8DebuggerToolHandler handler = getDebugContext().getV8Handler();
        handler.sendV8Command(DebuggerMessageFactory.backtrace(null, null, true), null);
        // Clear the scripts cache
        getDebugContext().getScriptManager().reset();
        handler.sendV8Command(
            DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true),
            null);
      }
    }
  }

  private void onBreakpointsHit(JSONObject reply) {
    JSONObject body = JsonUtil.getAsJSON(reply, V8Protocol.BREAK_BODY);
    JSONArray breakpointIdsArray = JsonUtil.getAsJSONArray(body, V8Protocol.BREAK_BREAKPOINTS);
    if (breakpointIdsArray == null) {
      // Suspended on step end.
      return;
    }
    Collection<Breakpoint> breakpointsHit = new ArrayList<Breakpoint>(breakpointIdsArray.size());
    for (int i = 0, size = breakpointIdsArray.size(); i < size; ++i) {
      Breakpoint existingBp = idToBreakpoint.get((breakpointIdsArray.get(i)));
      if (existingBp != null) {
        breakpointsHit.add(existingBp);
      }
    }
    getDebugContext().onBreakpointsHit(breakpointsHit);
  }

  public void setBreakpoint(Breakpoint.Type type, String target, int line, int position,
      final boolean enabled, final String condition, final int ignoreCount,
      final BrowserTab.BreakpointCallback callback) {
    getDebugContext().getV8Handler().sendV8Command(
        DebuggerMessageFactory.setBreakpoint(type, target, toNullableInteger(line),
            toNullableInteger(position), enabled, condition,
            toNullableInteger(ignoreCount)), callback == null
            ? null
            : new BrowserTabImpl.V8HandlerCallback() {
              public void messageReceived(JSONObject response) {
                if (JsonUtil.isSuccessful(response)) {
                  JSONObject body = JsonUtil.getBody(response);
                  Breakpoint.Type type = Breakpoint.Type.SCRIPT; // TODO(apavlov): implement others
                  int id = JsonUtil.getAsLong(body, V8Protocol.BODY_BREAKPOINT).intValue();

                  final BreakpointImpl breakpoint =
                      new BreakpointImpl(type, id, enabled, ignoreCount,
                          condition, BreakpointProcessor.this);

                  callback.success(breakpoint);
                  idToBreakpoint.put(breakpoint.getId(), breakpoint);
                } else {
                  callback.failure(JsonUtil.getAsString(response, V8Protocol.KEY_MESSAGE));
                }
              }
              public void failure(String message) {
                if (callback != null) {
                  callback.failure(message);
                }
              }
            });
    evaluateJavascript();
  }

  public void clearBreakpoint(
      final BreakpointImpl breakpointImpl, final BreakpointCallback callback) {
    int id = breakpointImpl.getId();
    if (id == Breakpoint.INVALID_ID) {
      return;
    }
    idToBreakpoint.remove(id);
    getDebugContext().getV8Handler().sendV8Command(
        DebuggerMessageFactory.clearBreakpoint(breakpointImpl), new BrowserTabImpl.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (JsonUtil.isSuccessful(response)) {
              if (callback != null) {
                callback.success(null);
              }
            } else {
              if (callback != null) {
                callback.failure(JsonUtil.getAsString(response, V8Protocol.KEY_MESSAGE));
              }
            }
          }
          public void failure(String message) {
            if (callback != null) {
              callback.failure(message);
            }
          }
        });
    evaluateJavascript();
  }

  public void changeBreakpoint(final BreakpointImpl breakpointImpl,
      final BreakpointCallback callback) {
    getDebugContext().getV8Handler().sendV8Command(
        DebuggerMessageFactory.changeBreakpoint(breakpointImpl), new BrowserTabImpl.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (callback != null) {
              if (JsonUtil.isSuccessful(response)) {
                  callback.success(breakpointImpl);
              } else {
                  callback.failure(JsonUtil.getAsString(response, V8Protocol.KEY_MESSAGE));
              }
            }
          }
          public void failure(String message) {
            if (callback != null) {
              callback.failure(message);
            }
          }
        });
    evaluateJavascript();
  }

  private void evaluateJavascript() {
    getDebugContext().getV8Handler().sendEvaluateJavascript("javascript:void(0);");
  }

  private static Integer toNullableInteger(int value) {
    return value == Breakpoint.NO_VALUE ? null : value;
  }

}
