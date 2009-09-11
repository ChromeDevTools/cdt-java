// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.InvalidContextException;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.JavascriptVm.ScriptsCallback;
import org.chromium.sdk.internal.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.tools.v8.BreakpointManager;
import org.chromium.sdk.internal.tools.v8.DefaultResponseHandler;
import org.chromium.sdk.internal.tools.v8.V8CommandOutput;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor.V8HandlerCallback;
import org.chromium.sdk.internal.tools.v8.request.ContextlessDebuggerMessage;
import org.json.simple.JSONObject;

/**
 * A default, thread-safe implementation of the JsDebugContext interface.
 */
public class DebugSession {

  /** The name of the "this" object to report as a variable name. */
  private static final String THIS_NAME = "this";

  /** The script manager for the associated tab. */
  private final ScriptManager scriptManager;

  private final V8CommandProcessor v8CommandProcessor;

  /** A helper for performing complex V8-related actions. */
  private final V8Helper v8Helper = new V8Helper(this, THIS_NAME);

  private final ContextBuilder contextBuilder;

  /** Our manager. */
  private DebugSessionManager sessionManager;

  /** Context owns breakpoint manager. */
  private final BreakpointManager breakpointManager;

  private final ScriptLoader scriptLoader = new ScriptLoader();

  public DebugSession(DebugSessionManager sessionManager, ProtocolOptions protocolOptions,
      V8CommandOutput v8CommandOutput) {
    this.scriptManager = new ScriptManager(protocolOptions);
    this.sessionManager = sessionManager;
    this.breakpointManager = new BreakpointManager(this);

    DefaultResponseHandler defaultResponseHandler = new DefaultResponseHandler(this);
    this.v8CommandProcessor = new V8CommandProcessor(v8CommandOutput, defaultResponseHandler);
    this.contextBuilder = new ContextBuilder(this);
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }

  public V8CommandProcessor getV8CommandProcessor() {
    return v8CommandProcessor;
  }

  public DebugSessionManager getSessionManager() {
    return sessionManager;
  }

  public void onDebuggerDetached() {
    getSessionManager().onDebuggerDetached();
    getScriptManager().reset();
    contextBuilder.forceCancelContext();
  }

  /**
   * Sends V8 command messages, but only those which doesn't depend on context.
   * Use {@code InternalContext} if you need to send context-specific commands.
   */
  public void sendMessageAsync(ContextlessDebuggerMessage message, boolean isImmediate,
      V8CommandProcessor.V8HandlerCallback commandCallback, SyncCallback syncCallback) {
    v8CommandProcessor.sendV8CommandAsync(message, isImmediate,
        commandCallback, syncCallback);
  }

  /**
   * Gets invoked when a navigation event is reported by the browser tab.
   */
  public void navigated() {
    getScriptManager().reset();
  }

  /**
   * @return the DebugEventListener associated with this context
   */
  public DebugEventListener getDebugEventListener() {
    return getSessionManager().getDebugEventListener();
  }

  public BreakpointManager getBreakpointManager() {
    return breakpointManager;
  }

  public ScriptLoader getScriptLoader() {
    return scriptLoader;
  }

  public V8Helper getV8Helper() {
    return v8Helper;
  }

  public ContextBuilder getContextBuilder() {
    return contextBuilder;
  }

  public class ScriptLoader {

    /** Whether the initial script loading has completed. */
    private volatile boolean doneInitialScriptLoad = false;

    /**
     * Loads all scripts from the remote if necessary, and feeds them into the
     * callback provided (if any).
     *
     * @param callback nullable callback to invoke when the scripts are ready
     */
    public void loadAllScripts(final ScriptsCallback callback, SyncCallback syncCallback) {
      if (!doneInitialScriptLoad) {
        this.doneInitialScriptLoad = true;
        // Not loaded the scripts initially, do full load.
        v8Helper.reloadAllScriptsAsync(new V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (callback != null) {
              if (JsonUtil.isSuccessful(response)) {
                callback.success(getScriptManager().allScripts());
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
        }, syncCallback);
      } else {
        try {
          if (callback != null) {
            callback.success(getScriptManager().allScripts());
          }
        } finally {
          if (syncCallback != null) {
            syncCallback.callbackDone(null);
          }
        }
      }
    }
  }

  public void maybeRethrowContextException(ContextDismissedCheckedException e) {
    // TODO(peter.rybin): make some kind of option out of this
    final boolean strictPolicy = true;
    if (strictPolicy) {
      throw new InvalidContextException(e);
    }
  }
}
