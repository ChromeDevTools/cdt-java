// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.InvalidContextException;
import org.chromium.sdk.JavascriptVm;
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
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFuture.Callback;

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

  private final ScriptLoader scriptLoader = new ScriptLoader(this);

  private final DefaultResponseHandler defaultResponseHandler;

  private final JavascriptVm javascriptVm;

  private volatile Version vmVersion = null;

  public DebugSession(DebugSessionManager sessionManager, V8ContextFilter contextFilter,
      V8CommandOutput v8CommandOutput, JavascriptVm javascriptVm) {
    this.scriptManager = new ScriptManager(contextFilter, this);
    this.sessionManager = sessionManager;
    this.javascriptVm = javascriptVm;
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

  JavascriptVm getJavascriptVm() {
    return javascriptVm;
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

  public static class ScriptLoader {
    private final DebugSession debugSession;
    private final AtomicReference<AsyncFuture<ScriptResult>> futureRef =
        new AtomicReference<AsyncFuture<ScriptResult>>(null);

    ScriptLoader(DebugSession debugSession) {
      this.debugSession = debugSession;
    }

    public void loadAllScripts(final ScriptsCallback callback, SyncCallback syncCallback) {
      AsyncFuture<ScriptResult> future = futureRef.get();
      if (future == null) {
        AsyncFuture.initializeReference(futureRef, new ScriptsRequester());
        future = futureRef.get();
      }
      future.getAsync(new Callback<ScriptResult>() {
            @Override
            public void done(ScriptResult res) {
              if (callback != null) {
                res.accept(callback);
              }
            }
          },
          syncCallback);
    }

    private static abstract class ScriptResult {
      abstract void accept(ScriptsCallback callback);
    }

    private class ScriptsRequester implements AsyncFuture.Operation<ScriptResult> {
      @Override
      public void start(final Callback<ScriptResult> requestCallback,
          SyncCallback syncCallback) {
        V8Helper.ScriptLoadCallback scriptLoadCallback = new V8Helper.ScriptLoadCallback() {
          @Override
          public void success() {
            final Collection<Script> scripts = debugSession.scriptManager.allScripts();
            requestCallback.done(new ScriptResult() {
              @Override
              void accept(ScriptsCallback callback) {
                callback.success(scripts);
              }
            });
          }

          @Override
          public void failure(final String message) {
            requestCallback.done(new ScriptResult() {
              @Override
              void accept(ScriptsCallback callback) {
                callback.failure(message);
              }
            });
          }
        };
        V8Helper.reloadAllScriptsAsync(debugSession, scriptLoadCallback, syncCallback);
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
