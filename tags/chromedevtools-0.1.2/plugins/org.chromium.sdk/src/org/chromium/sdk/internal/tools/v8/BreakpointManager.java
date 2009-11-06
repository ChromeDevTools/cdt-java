package org.chromium.sdk.internal.tools.v8;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

public class BreakpointManager {
  /**
   * This map shall contain only breakpoints with valid IDs.
   */
  private final Map<Long, Breakpoint> idToBreakpoint = new HashMap<Long, Breakpoint>();

  private final DebugSession debugSession;

  public BreakpointManager(DebugSession debugSession) {
    this.debugSession = debugSession;
  }

  public void setBreakpoint(final Breakpoint.Type type, String target, int line, int position,
      final boolean enabled, final String condition, final int ignoreCount,
      final BrowserTab.BreakpointCallback callback) {
    debugSession.sendMessageAsync(
        DebuggerMessageFactory.setBreakpoint(type, target, toNullableInteger(line),
            toNullableInteger(position), enabled, condition,
            toNullableInteger(ignoreCount)),
        true,
        callback == null
            ? null
            : new V8CommandProcessor.V8HandlerCallback() {
              public void messageReceived(JSONObject response) {
                if (JsonUtil.isSuccessful(response)) {
                  JSONObject body = JsonUtil.getBody(response);
                  long id = JsonUtil.getAsLong(body, V8Protocol.BODY_BREAKPOINT);

                  final BreakpointImpl breakpoint =
                      new BreakpointImpl(type, id, enabled, ignoreCount,
                          condition, BreakpointManager.this);

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
            },
            null);
  }

  public Breakpoint getBreakpoint(Long id) {
    return idToBreakpoint.get(id);
  }

  public void clearBreakpoint(
      final BreakpointImpl breakpointImpl, final BreakpointCallback callback) {
    long id = breakpointImpl.getId();
    if (id == Breakpoint.INVALID_ID) {
      return;
    }
    idToBreakpoint.remove(id);
    debugSession.sendMessageAsync(
        DebuggerMessageFactory.clearBreakpoint(breakpointImpl),
        true,
        new V8CommandProcessor.V8HandlerCallback() {
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
        },
        null);
  }

  public void changeBreakpoint(final BreakpointImpl breakpointImpl,
      final BreakpointCallback callback) {
    debugSession.sendMessageAsync(
        DebuggerMessageFactory.changeBreakpoint(breakpointImpl),
        true,
        new V8CommandProcessor.V8HandlerCallback() {
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
        },
        null);
  }

  private static Integer toNullableInteger(int value) {
    return value == Breakpoint.EMPTY_VALUE
        ? null
        : value;
  }
}
