// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.InvalidContextException;
import org.chromium.sdk.Script;
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
import org.chromium.sdk.internal.tools.v8.V8CommandCallbackBase;
import org.chromium.sdk.internal.tools.v8.V8CommandOutput;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.ContextlessDebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;

/**
 * A class that holds and administers main parts of debug protocol implementation.
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

  private volatile Version vmVersion = null;

  public DebugSession(DebugSessionManager sessionManager, V8ContextFilter contextFilter,
      V8CommandOutput v8CommandOutput) {
    this.scriptManager = new ScriptManager(contextFilter, this);
    this.sessionManager = sessionManager;
    this.breakpointManager = new BreakpointManager(this);

    this.defaultResponseHandler = new DefaultResponseHandler(this);
    this.v8CommandProcessor = new V8CommandProcessor(v8CommandOutput, defaultResponseHandler,
        this);
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

  Version getVmVersion() {
    return vmVersion;
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

  /**
   * Drops current context and creates a new one. This is useful if context is known to have changed
   * (e.g. experimental feature LiveEdit may change current stack while execution is suspended).
   * The method is asynchronous and returns immediately.
   * Does nothing if currently there is no active context. Otherwise dismisses current context,
   * invokes {@link DebugEventListener#resumed()} and initiates downloading stack frame descriptions
   * and building new context. When the context is built,
   * calls {@link DebugEventListener#suspended(DebugContext)}.
   * <p>
   * Must be called from Dispatch Thread.
   * @return true if context has been actually dropped.
   */
  public boolean recreateCurrentContext() {
    ContextBuilder.ExpectingBacktraceStep step = contextBuilder.startRebuildCurrentContext();
    if (step == null) {
      return false;
    }
    defaultResponseHandler.getBreakpointProcessor().processNextStep(step);
    return true;
  }

  public void suspend(final SuspendCallback suspendCallback) {
    V8CommandProcessor.V8HandlerCallback v8Callback = new V8CommandCallbackBase() {
      @Override
      public void failure(String message) {
        if (suspendCallback != null) {
          suspendCallback.failure(new Exception(message));
        }
      }
      @Override
      public void success(SuccessCommandResponse successResponse) {
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

  public class ScriptLoader {

    private final Object monitor = new Object();
    /**
     * Stores the callbacks that are waiting for result.
     * This field being reset to null means that result is ready (loaded into ScriptManager)
     * and no more callbacks are accepted.
     */
    private List<ScriptsCallback> pendingCallbacks = new ArrayList<ScriptsCallback>(2);
    private List<SyncCallback> pendingSyncCallbacks = new ArrayList<SyncCallback>(2);

    /**
     * Loads all scripts from the remote if necessary, and feeds them into the
     * callback provided (if any).
     */
    public void loadAllScripts(ScriptsCallback callback, SyncCallback syncCallback) {
      boolean resultIsReady;
      boolean sendMessage;
      synchronized (monitor) {
        if (pendingCallbacks == null) {
          resultIsReady = true;
          sendMessage = false;
        } else {
          resultIsReady = false;
          sendMessage = pendingCallbacks.isEmpty();
          pendingCallbacks.add(callback);
          pendingSyncCallbacks.add(syncCallback);
        }
      }
      if (resultIsReady) {
        try {
          if (callback != null) {
            callback.success(getScriptManager().allScripts());
          }
        } finally {
          if (syncCallback != null) {
            syncCallback.callbackDone(null);
          }
        }
        return;
      }
      if (sendMessage) {
        sendAsyncMessage();
      }
    }
    private void sendAsyncMessage() {
      V8Helper.ScriptLoadCallback groupCallback = new V8Helper.ScriptLoadCallback() {
        public void success() {
          final Collection<Script> scripts = scriptManager.allScripts();
          processCall(new CallbackCaller() {
            @Override
            void call(ScriptsCallback callback) {
              callback.success(scripts);
            }
          });
        }

        public void failure(final String message) {
          processCall(new CallbackCaller() {
            @Override
            void call(ScriptsCallback callback) {
              callback.failure(message);
            }
          });
        }

        private void processCall(CallbackCaller caller) {
          List<ScriptsCallback> savedCallbacks;
          synchronized (monitor) {
            savedCallbacks = pendingCallbacks;
            pendingCallbacks = null;
          }
          for (ScriptsCallback callback : savedCallbacks) {
            if (callback != null) {
              caller.call(callback);
            }
          }
        }
        abstract class CallbackCaller {
          abstract void call(ScriptsCallback callback);
        }
      };

      SyncCallback groupSyncCallback = new SyncCallback() {
        public void callbackDone(RuntimeException e) {
          List<SyncCallback> savedCallbacks;
          synchronized (monitor) {
            savedCallbacks = pendingSyncCallbacks;
            pendingSyncCallbacks = null;
          }
          for (SyncCallback callback : savedCallbacks) {
            if (callback != null) {
              callback.callbackDone(e);
            }
          }
        }
      };

      V8Helper.reloadAllScriptsAsync(DebugSession.this, groupCallback, groupSyncCallback);
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
        DebugSession.this.vmVersion = vmVersion;

        if (V8VersionFeatures.isRunningAccurate(vmVersion)) {
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

  public void sendLoopbackMessage(Runnable callback, SyncCallback syncCallback) {
    this.v8CommandProcessor.runInDispatchThread(callback, syncCallback);
  }

  public void maybeRethrowContextException(ContextDismissedCheckedException e) {
    // TODO(peter.rybin): make some kind of option out of this
    final boolean strictPolicy = true;
    if (strictPolicy) {
      throw new InvalidContextException(e);
    }
  }
}
