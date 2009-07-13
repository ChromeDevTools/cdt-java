// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.BrowserTab.BreakpointCallback;
import org.chromium.sdk.DebugContext.State;
import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.DebugContextImpl.SendingType;
import org.chromium.sdk.internal.tools.v8.BreakpointImpl;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
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
  private final Map<Long, Breakpoint> idToBreakpoint = new HashMap<Long, Breakpoint>();

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
        debugContext.onBreakpointsHit(Collections.<Breakpoint>emptySet());
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
    if (breakpointIdsArray == null) {
      // Suspended on step end.
      getDebugContext().onBreakpointsHit(Collections.<Breakpoint>emptySet());
      return;
    }
    Collection<Breakpoint> breakpointsHit = new ArrayList<Breakpoint>(breakpointIdsArray.size());
    for (int i = 0, size = breakpointIdsArray.size(); i < size; ++i) {
      Breakpoint existingBp = idToBreakpoint.get(breakpointIdsArray.get(i));
      if (existingBp != null) {
        breakpointsHit.add(existingBp);
      }
    }
    getDebugContext().onBreakpointsHit(breakpointsHit);
  }

  public void setBreakpoint(final Breakpoint.Type type, String target, int line, int position,
      final boolean enabled, final String condition, final int ignoreCount,
      final BrowserTab.BreakpointCallback callback) {
    getDebugContext().sendMessage(
        SendingType.ASYNC_IMMEDIATE,
        DebuggerMessageFactory.setBreakpoint(type, target, toNullableInteger(line),
            toNullableInteger(position), enabled, condition,
            toNullableInteger(ignoreCount)),
        callback == null
            ? null
            : new BrowserTabImpl.V8HandlerCallback() {
              public void messageReceived(JSONObject response) {
                if (JsonUtil.isSuccessful(response)) {
                  JSONObject body = JsonUtil.getBody(response);
                  long id = JsonUtil.getAsLong(body, V8Protocol.BODY_BREAKPOINT);

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
  }

  public void clearBreakpoint(
      final BreakpointImpl breakpointImpl, final BreakpointCallback callback) {
    long id = breakpointImpl.getId();
    if (id == Breakpoint.INVALID_ID) {
      return;
    }
    idToBreakpoint.remove(id);
    getDebugContext().sendMessage(
        SendingType.ASYNC_IMMEDIATE,
        DebuggerMessageFactory.clearBreakpoint(breakpointImpl),
        new BrowserTabImpl.V8HandlerCallback() {
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
  }

  public void changeBreakpoint(final BreakpointImpl breakpointImpl,
      final BreakpointCallback callback) {
    getDebugContext().sendMessage(
        SendingType.ASYNC_IMMEDIATE,
        DebuggerMessageFactory.changeBreakpoint(breakpointImpl),
        new BrowserTabImpl.V8HandlerCallback() {
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
  }

  private static Integer toNullableInteger(int value) {
    return value == Breakpoint.NO_VALUE
        ? null
        : value;
  }

}
