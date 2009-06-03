// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler.AttachmentFailureException;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.ScriptsMessage;
import org.json.simple.JSONObject;

/**
 * A default, thread-safe implementation of the BrowserTab interface.
 */
public class BrowserTabImpl implements BrowserTab {

  /**
   * A callback to handle V8 debugger responses.
   */
  public interface V8HandlerCallback {

    /**
     * This method is invoked when a debugger command result has become
     * available.
     *
     * @param response from the V8 debugger
     */
    void messageReceived(JSONObject response);

    /**
     * This method is invoked when a debugger command has failed.
     *
     * @param message containing the failure reason
     */
    void failure(String message);
  }

  /** Tab ID as reported by the DevTools server. */
  private final int tabId;

  /** The primary tab URL. */
  private final String url;

  /** The host BrowserImpl instance. */
  private final BrowserImpl browserImpl;

  /** The debug context instance for this tab. */
  private final DebugContextImpl context;

  /** The listener to report debug events to. */
  private DebugEventListener debugEventListener;

  public BrowserTabImpl(int tabId, String url, BrowserImpl browserImpl) {
    this.tabId = tabId;
    this.url = url;
    this.browserImpl = browserImpl;
    this.context = new DebugContextImpl(this);
  }

  @Override
  public String getUrl() {
    return url;
  }

  public int getId() {
    return tabId;
  }

  public DebugContextImpl getDebugContext() {
    return context;
  }

  public synchronized DebugEventListener getDebugEventListener() {
    return debugEventListener;
  }

  @Override
  public BrowserImpl getBrowser() {
    return browserImpl;
  }

  @Override
  public synchronized boolean attach(DebugEventListener listener) {
    Result result = null;
    try {
      result = getDebugContext().getV8Handler().attachToTab();
    } catch (AttachmentFailureException e1) {
      listener.disconnected();
    }
    this.debugEventListener = listener;
    return Result.OK == result;
  }

  @Override
  public synchronized boolean detach() {
    final Result result = getDebugContext().getV8Handler().detachFromTab();
    return Result.OK == result;
  }

  @Override
  public boolean isAttached() {
    return context.getV8Handler().isAttached();
  }

  @Override
  public void getScripts(final ScriptsCallback callback) {
    context.getV8Handler().sendV8Command(
        DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true),
        new V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (callback != null) {
              if (JsonUtil.isSuccessful(response)) {
                callback.success(context.getScriptManager().allScripts());
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
    context.getV8Handler().sendEvaluateJavascript("javascript:void(0);");
  }

  @Override
  public void setBreakpoint(Breakpoint.Type type, String target, int line,
      int position, boolean enabled, String condition, int ignoreCount,
      final BreakpointCallback callback) {
    context.getV8Handler().getBreakpointProcessor().setBreakpoint(type, target, line,
        position, enabled, condition, ignoreCount, callback);
  }

  public void sessionTerminated() {
    browserImpl.sessionTerminated(this.tabId);
  }

}
