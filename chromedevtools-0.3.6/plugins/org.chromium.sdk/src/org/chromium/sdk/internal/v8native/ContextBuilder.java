// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.V8CommandProcessor.V8HandlerCallback;
import org.chromium.sdk.internal.v8native.protocol.input.FrameObject;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessageFactory;
import org.chromium.sdk.internal.v8native.value.ValueLoaderImpl;

public class ContextBuilder {
  private final DebugSession debugSession;

  /**
   * Context building process comes though sequence of steps. No one should
   * be able to work with any step exception the current one.
   */
  private Object currentStep = null;

  ContextBuilder(DebugSession debugSession) {
    this.debugSession = debugSession;
  }

  public interface ExpectingBreakEventStep {
    InternalContext getInternalContext();
    /**
     * Stores the breakpoints associated with V8 suspension event (empty if an
     * exception or a step end).
     *
     * @param breakpointsHit the breakpoints that were hit
     */
    ExpectingBacktraceStep setContextState(Collection<Breakpoint> breakpointsHit,
        ExceptionData exceptionData);
  }
  public interface ExpectingBacktraceStep {
    InternalContext getInternalContext();

    DebugContext setFrames(List<FrameObject> jsonFrames);
  }

  /**
   * Starting point of building new DebugContext process. One should traverse
   * list of steps to get the result.
   * @return object representing first step of context building process
   */
  public ExpectingBreakEventStep buildNewContext() {
    assertStep(null);

    final PreContext preContext = new PreContext();
    final DebugContextData contextData = new DebugContextData();

    return new ExpectingBreakEventStep() {
      {
        currentStep = this;
      }

      public InternalContext getInternalContext() {
        return preContext;
      }

      public ExpectingBacktraceStep setContextState(Collection<Breakpoint> breakpointsHit,
          ExceptionData exceptionData) {
        assertStep(this);

        DebugContext.State state;
        if (exceptionData == null) {
          state = DebugContext.State.NORMAL;
        } else {
          state = DebugContext.State.EXCEPTION;
        }

        contextData.contextState = state;
        contextData.breakpointsHit = Collections.unmodifiableCollection(breakpointsHit);
        contextData.exceptionData = exceptionData;

        return new ExpectingBacktraceStep() {
          {
            currentStep = this;
          }

          public InternalContext getInternalContext() {
            return preContext;
          }

          public DebugContext setFrames(List<FrameObject> jsonFrames) {
            assertStep(this);

            contextData.frames = new Frames(jsonFrames, preContext);

            preContext.createContext(contextData);

            DebugContext userContext = preContext.getContext();
            currentStep = userContext;
            return userContext;
          }
        };
      }
    };
  }

  public ExpectingBreakEventStep buildNewContextWhenIdle() {
    if (currentStep == null) {
      return buildNewContext();
    } else {
      return null;
    }
  }

  /**
   * Debug session is stopped. Cancel context in any state.
   */
  public void forceCancelContext() {
    // TODO(peter.rybin): complete it
  }

  public void buildSequenceFailure() {
    // this means we can't go on debugging
    // TODO(peter.rybin): implement
    throw new RuntimeException();
  }

  private void contextDismissed(DebugContext userContext) {
    assertStep(userContext);
    currentStep = null;
  }

  private void assertStep(Object step) {
    if (currentStep != step) {
      throw new IllegalStateException("Expected " + step + ", but was " + currentStep);
    }
  }

  private class PreContext implements InternalContext {
    private final ValueLoaderImpl valueLoader = new ValueLoaderImpl(this);

    /**
     * We synchronize {@link #isValid} state with commands that are being sent
     * using this monitor.
     */
    private final Object sendContextCommandsMonitor = new Object();
    private volatile boolean isValid = true;
    private UserContextImpl context = null;

    public boolean isValid() {
      return isValid;
    }

    public DebugSession getDebugSession() {
      return debugSession;
    }

    public ContextBuilder getContextBuilder() {
      return ContextBuilder.this;
    }

    @Override
    public UserContext getUserContext() {
      return context;
    }

    void assertValid() {
      if (!isValid) {
        throw new IllegalStateException("This instance of DebugContext cannot be used anymore");
      }
    }

    /**
     * Check if context is valid right now. Throws exception if we are in a strict mode.
     * Ignores otherwise.
     */
    void assertValidForUser() {
      if (!isValid) {
        debugSession.maybeRethrowContextException(null);
      }
    }

    public UserContextImpl getContext() {
      if (context == null) {
        throw new IllegalStateException();
      }
      return context;
    }

    public CallFrameImpl getTopFrameImpl() {
      assertValid();
      return getContext().data.frames.getCallFrames().get(0);
    }

    public ValueLoaderImpl getValueLoader() {
      return valueLoader;
    }


    void createContext(DebugContextData contextData) {
      if (context != null) {
        throw new IllegalStateException();
      }
      context = new UserContextImpl(contextData);
    }

    @Override
    public RelayOk sendV8CommandAsync(DebuggerMessage message, boolean isImmediate,
        V8HandlerCallback commandCallback, SyncCallback syncCallback)
        throws ContextDismissedCheckedException {
      synchronized (sendContextCommandsMonitor) {
        if (!isValid) {
          throw new ContextDismissedCheckedException();
        }
        return debugSession.getV8CommandProcessor().sendV8CommandAsync(message, isImmediate,
            commandCallback, syncCallback);
      }
    }

    private RelayOk sendMessageAsyncAndInvalidate(DebuggerMessage message,
        V8CommandProcessor.V8HandlerCallback commandCallback, boolean isImmediate,
        SyncCallback syncCallback) {
      synchronized (sendContextCommandsMonitor) {
        assertValid();
        RelayOk relayOk = debugSession.getV8CommandProcessor().sendV8CommandAsync(message,
            isImmediate, commandCallback, syncCallback);
        isValid = false;
        return relayOk;
      }
    }

    private boolean sendNoMessageAndInvalidate() {
      synchronized (sendContextCommandsMonitor) {
        if (!isValid) {
          return false;
        }
        isValid = false;
        return true;
      }
    }


    private class UserContextImpl implements UserContext {
      private final DebugContextData data;

      public UserContextImpl(DebugContextData contextData) {
        this.data = contextData;
      }

      @Override
      public State getState() {
        assertValidForUser();
        return data.contextState;
      }

      @Override
      public List<? extends CallFrame> getCallFrames() {
        assertValidForUser();
        return data.frames.getCallFrames();
      }

      @Override
      public Collection<Breakpoint> getBreakpointsHit() {
        assertValidForUser();
        if (data.breakpointsHit == null) {
          throw new RuntimeException();
        }
        return data.breakpointsHit;
      }

      @Override
      public ExceptionData getExceptionData() {
        assertValidForUser();
        return data.exceptionData;
      }

      Collection<Breakpoint> getBreakpointsHitSafe() {
        return data.breakpointsHit;
      }

      ExceptionData getExceptionDataSafe() {
        return data.exceptionData;
      }

      @Override
      public JsEvaluateContext getGlobalEvaluateContext() {
        return evaluateContext;
      }

      /**
       * @throws IllegalStateException if context has already been continued
       */
      @Override
      public void continueVm(StepAction stepAction, int stepCount,
          final ContinueCallback callback) {
        continueVm(stepAction, stepCount, callback, null);
      }

      @Override
      public RelayOk continueVm(StepAction stepAction, int stepCount,
          final ContinueCallback callback, SyncCallback syncCallback) {
        if (stepAction == null) {
          throw new NullPointerException();
        }

        DebuggerMessage message = DebuggerMessageFactory.goOn(stepAction, stepCount);
        V8CommandProcessor.V8HandlerCallback commandCallback
            = new V8CommandCallbackBase() {
          @Override
          public void success(SuccessCommandResponse successResponse) {
            contextDismissed(UserContextImpl.this);

            if (callback != null) {
              callback.success();
            }
            getDebugSession().getDebugEventListener().resumed();
          }
          @Override
          public void failure(String message) {
            synchronized (sendContextCommandsMonitor) {
              // resurrected
              isValid = true;
            }
            if (callback != null) {
              callback.failure(message);
            }
          }
        };

        return sendMessageAsyncAndInvalidate(message, commandCallback, true, syncCallback);
      }

      @Override
      public JavascriptVm getJavascriptVm() {
        return debugSession.getJavascriptVm();
      }

      @Override
      public RemoteValueMapping getDefaultRemoteValueMapping() {
        return valueLoader;
      }

      /**
       * Behaves as continue but does not send any messages to remote VM.
       * This brings ContextBuilder to state when we are about to build a new context.
       */
      boolean continueLocally() {
        if (!sendNoMessageAndInvalidate()) {
          return false;
        }
        contextDismissed(UserContextImpl.this);
        getDebugSession().getDebugEventListener().resumed();
        return true;
      }

      @Override public InternalContext getInternalContext() {
        return PreContext.this;
      }

      InternalContext getInternalContextForTests() {
        return PreContext.this;
      }

      @Override
      public ExpectingBacktraceStep createReloadBacktraceStep() {
        return new ExpectingBacktraceStep() {
          @Override public InternalContext getInternalContext() {
            return PreContext.this;
          }

          @Override
          public DebugContext setFrames(List<FrameObject> jsonFrames) {
            data.frames = new Frames(jsonFrames, PreContext.this);
            return UserContextImpl.this;
          }
        };
      }

      private final JsEvaluateContext evaluateContext = new JsEvaluateContextImpl() {
        @Override
        protected Integer getFrameIdentifier() {
          return null;
        }
        @Override
        public InternalContext getInternalContext() {
          return PreContext.this;
        }
      };
    }
  }

  /**
   * Simple structure of data which DebugConext implementation uses.
   */
  private static class DebugContextData {
    private Frames frames;
    /** The breakpoints hit before suspending. */
    private volatile Collection<Breakpoint> breakpointsHit;

    DebugContext.State contextState;
    /** The JavaScript exception state. */
    private ExceptionData exceptionData;
  }

  private class Frames {
    /** The cached call frames constructed using frameMirrors. */
    private final List<CallFrameImpl> unmodifableFrames;
    private boolean scriptsLinkedToFrames;

    Frames(List<FrameObject> jsonFrames, InternalContext internalContext) {
      CallFrameImpl[] callFrames = new CallFrameImpl[jsonFrames.size()];

      for (FrameObject frameObject : jsonFrames) {
        CallFrameImpl callFrameImpl = new CallFrameImpl(frameObject, internalContext);
        callFrames[callFrameImpl.getIdentifier()] = callFrameImpl;
      }

      this.scriptsLinkedToFrames = false;
      this.unmodifableFrames = Collections.unmodifiableList(Arrays.asList(callFrames));
    }

    synchronized List<CallFrameImpl> getCallFrames() {
      if (!scriptsLinkedToFrames) {
        // We expect that ALL the V8 scripts are loaded so we can
        // hook them up to the call frames.
        for (CallFrameImpl frame : unmodifableFrames) {
          frame.hookUpScript(debugSession.getScriptManager());
        }
        scriptsLinkedToFrames = true;
      }
      return unmodifableFrames;
    }
  }

  /**
   * Must be called from Dispatch thread.
   */
  private PreContext.UserContextImpl getCurrentUserContext() {
    // We can use currentStep as long as we are being operated from Dispatch thread.
    if (currentStep instanceof PreContext.UserContextImpl == false) {
      return null;
    }
    PreContext.UserContextImpl userContext = (PreContext.UserContextImpl) currentStep;
    return userContext;
  }

  /**
   * Must be called from Dispatch thread.
   * @return current context instance or null if there's no active context at the moment
   */
  PreContext.UserContextImpl getCurrentDebugContext() {
    return getCurrentUserContext();
  }

  /**
   * Drops the current context and initiates reading data and building a new one.
   * Must be called from Dispatch thread.
   * @return ExpectingBacktraceStep or null if there is no active context currently
   */
  ExpectingBacktraceStep startRebuildCurrentContext() {
    PreContext.UserContextImpl userContext = getCurrentUserContext();
    if (userContext == null) {
      return null;
    }
    if (!userContext.continueLocally()) {
      return null;
    }
    return buildNewContext().setContextState(userContext.getBreakpointsHitSafe(),
        userContext.getExceptionDataSafe());
  }

  public static InternalContext getInternalContextForTests(DebugContext debugContext) {
    PreContext.UserContextImpl userContext = (PreContext.UserContextImpl) debugContext;
    return userContext.getInternalContextForTests();
  }
}
