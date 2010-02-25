// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collections;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.InvalidContextException;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.Version;
import org.chromium.sdk.JavascriptVm.ScriptsCallback;
import org.chromium.sdk.JavascriptVm.SuspendCallback;
import org.chromium.sdk.internal.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.protocol.CommandResponse;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.tools.v8.BreakpointManager;
import org.chromium.sdk.internal.tools.v8.DefaultResponseHandler;
import org.chromium.sdk.internal.tools.v8.V8BlockingCallback;
import org.chromium.sdk.internal.tools.v8.V8CommandOutput;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor.V8HandlerCallback;
import org.chromium.sdk.internal.tools.v8.request.ContextlessDebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;

/**
 * A default, thread-safe implementation of the JsDebugContext interface.
 */
public class DebugSession {

  /** The script manager for the associated tab. */
  private final ScriptManager scriptManager;

  private final V8CommandProcessor v8CommandProcessor;

  /** A helper for performing complex V8-related actions. */
  private final V8Helper v8Helper = new V8Helper(this);

  private final ContextBuilder contextBuilder;

  /** Our manager. */
  private DebugSessionManager sessionManager;

  /** Context owns breakpoint manager. */
  private final BreakpointManager breakpointManager;

  private final ScriptLoader scriptLoader = new ScriptLoader();

  private final DefaultResponseHandler defaultResponseHandler;

  public DebugSession(DebugSessionManager sessionManager, V8ContextFilter contextFilter,
      V8CommandOutput v8CommandOutput) {
    this.scriptManager = new ScriptManager(contextFilter);
    this.sessionManager = sessionManager;
    this.breakpointManager = new BreakpointManager(this);

    this.defaultResponseHandler = new DefaultResponseHandler(this);
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

  public void suspend(final SuspendCallback suspendCallback) {
    V8CommandProcessor.V8HandlerCallback v8Callback = new V8CommandProcessor.V8HandlerCallback() {
      public void failure(String message) {
        if (suspendCallback != null) {
          suspendCallback.failure(new Exception(message));
        }
      }
      public void messageReceived(CommandResponse response) {
        SuccessCommandResponse successResponse = response.asSuccess();
        if (successResponse == null) {
          if (suspendCallback != null) {
            suspendCallback.failure(new Exception("Unsuccessful command"));
          }
          return;
        }
        if (suspendCallback != null) {
          suspendCallback.success();
        }

        ContextBuilder.ExpectingBreakEventStep step1 = contextBuilder.buildNewContextWhenIdle();
        if (step1 == null) {
          return;
        }
        ContextBuilder.ExpectingBacktraceStep step2 =
            step1.setContextState(Collections.<Breakpoint>emptyList(), null);
        defaultResponseHandler.getBreakpointProcessor().processNextStep(step2);
      }
    };
    sendMessageAsync(DebuggerMessageFactory.suspend(), true, v8Callback, null);
  }

  // TODO(peter.rybin): clean up this stuff; loadAllScripts is obviously not reentrant.
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
          public void messageReceived(CommandResponse response) {
            if (callback != null) {
              SuccessCommandResponse successResponse = response.asSuccess();
              if (successResponse != null) {
                callback.success(getScriptManager().allScripts());
              } else {
                callback.failure(response.asFailure().getMessage());
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

  /**
   * Checks version of V8 and check if it in running state.
   */
  public void startCommunication() {
    V8BlockingCallback<Void> callback = new V8BlockingCallback<Void>() {
      @Override
      public Void messageReceived(CommandResponse response) {
        SuccessCommandResponse successResponse = response.asSuccess();
        if (successResponse == null) {
          return null;
        }
        Version vmVersion = V8ProtocolUtil.parseVersionResponse(successResponse);

        if (V8VersionMilestones.isRunningAccurate(vmVersion)) {
          Boolean running = successResponse.running();
          if (running == Boolean.FALSE) {
            ContextBuilder.ExpectingBreakEventStep step1 = contextBuilder.buildNewContextWhenIdle();
            // If step is not null -- we are already in process of building a context.
            if (step1 != null) {
              ContextBuilder.ExpectingBacktraceStep step2 =
                  step1.setContextState(Collections.<Breakpoint>emptyList(), null);

              defaultResponseHandler.getBreakpointProcessor().processNextStep(step2);
            }
          }
        }
        return null;
      }

      @Override
      protected Void handleSuccessfulResponse(SuccessCommandResponse response) {
        throw new UnsupportedOperationException();
      }
    };

    V8Helper.callV8Sync(this.v8CommandProcessor, DebuggerMessageFactory.version(), callback);
  }

  public void maybeRethrowContextException(ContextDismissedCheckedException e) {
    // TODO(peter.rybin): make some kind of option out of this
    final boolean strictPolicy = true;
    if (strictPolicy) {
      throw new InvalidContextException(e);
    }
  }
}
