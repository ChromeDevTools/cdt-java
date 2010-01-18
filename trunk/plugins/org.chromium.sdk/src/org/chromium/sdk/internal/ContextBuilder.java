// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocol.CommandResponse;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor.V8HandlerCallback;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;

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

    DebugContext setFrames(FrameMirror[] frameMirrors);
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

          public DebugContext setFrames(FrameMirror[] frameMirrors) {
            assertStep(this);

            contextData.frames = new Frames(frameMirrors, preContext);

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
    private final HandleManager handleManager = new HandleManager();
    private final ValueLoader valueLoader = new ValueLoader(this);

    /**
     * We synchronize {@link #isValid} state with commands that are being sent
     * using this monitor.
     */
    private final Object sendContextCommandsMonitor = new Object();
    private volatile boolean isValid = true;
    private UserContext context = null;

    public boolean isValid() {
      return isValid;
    }

    public DebugSession getDebugSession() {
      return debugSession;
    }

    public ContextBuilder getContextBuilder() {
      return ContextBuilder.this;
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

    public UserContext getContext() {
      if (context == null) {
        throw new IllegalStateException();
      }
      return context;
    }

    public CallFrameImpl getTopFrameImpl() {
      assertValid();
      return getContext().data.frames.getCallFrames().get(0);
    }

    public HandleManager getHandleManager() {
      // tolerates dismissed context
      return handleManager;
    }

    public ValueLoader getValueLoader() {
      return valueLoader;
    }


    void createContext(DebugContextData contextData) {
      if (context != null) {
        throw new IllegalStateException();
      }
      context = new UserContext(contextData);
    }

    public void sendV8CommandAsync(DebuggerMessage message, boolean isImmediate,
        V8HandlerCallback commandCallback, SyncCallback syncCallback)
        throws ContextDismissedCheckedException {
      synchronized (sendContextCommandsMonitor) {
        if (!isValid) {
          throw new ContextDismissedCheckedException();
        }
        debugSession.getV8CommandProcessor().sendV8CommandAsync(message, isImmediate,
            commandCallback, syncCallback);
      }
    }

    private void sendMessageAsyncAndIvalidate(DebuggerMessage message,
        V8CommandProcessor.V8HandlerCallback commandCallback, boolean isImmediate,
        SyncCallback syncCallback) {
      synchronized (sendContextCommandsMonitor) {
        assertValid();
        debugSession.getV8CommandProcessor().sendV8CommandAsync(message, isImmediate,
            commandCallback, syncCallback);
        isValid = false;
      }
    }

    private class UserContext implements DebugContext {
      private final DebugContextData data;

      public UserContext(DebugContextData contextData) {
        this.data = contextData;
      }

      public State getState() {
        assertValidForUser();
        return data.contextState;
      }
      public List<? extends CallFrame> getCallFrames() {
        assertValidForUser();
        return data.frames.getCallFrames();
      }

      public Collection<Breakpoint> getBreakpointsHit() {
        assertValidForUser();
        if (data.breakpointsHit == null) {
          throw new RuntimeException();
        }
        return data.breakpointsHit;
      }

      public ExceptionData getExceptionData() {
        assertValidForUser();
        return data.exceptionData;
      }

      /**
       * @throws IllegalStateException if context has already been continued
       */
      public void continueVm(StepAction stepAction, int stepCount,
          final ContinueCallback callback) {
        if (stepAction == null) {
          throw new NullPointerException();
        }

        DebuggerMessage message = DebuggerMessageFactory.goOn(stepAction, stepCount);
        V8CommandProcessor.V8HandlerCallback commandCallback
            = new V8CommandProcessor.V8HandlerCallback() {
          public void messageReceived(CommandResponse response) {
            SuccessCommandResponse successResponse = response.asSuccess();
            if (successResponse == null) {
              this.failure(response.asFailure().getMessage());
              return;
            }

            contextDismissed(UserContext.this);

            if (callback != null) {
              callback.success();
            }
            getDebugSession().getDebugEventListener().resumed();
          }
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

        sendMessageAsyncAndIvalidate(message, commandCallback, true, null);
      }

      public JsEvaluateContext getGlobalEvaluateContext() {
        throw new UnsupportedOperationException();
      }

      InternalContext getInternalContextForTests() {
        return PreContext.this;
      }
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
    /** The frame mirrors while on a breakpoint. */
    private final FrameMirror[] frameMirrors;
    /** The cached call frames constructed using frameMirrors. */
    private final List<CallFrameImpl> unmodifableFrames;
    private boolean scriptsLinkedToFrames;

    Frames(FrameMirror[] frameMirrors0, InternalContext internalContext) {
      this.frameMirrors = frameMirrors0;
      this.scriptsLinkedToFrames = false;

      int frameCount = frameMirrors.length;
      List<CallFrameImpl> frameList = new ArrayList<CallFrameImpl>(frameCount);
      for (int i = 0; i < frameCount; ++i) {
        frameList.add(new CallFrameImpl(frameMirrors[i], i, internalContext));
      }
      this.unmodifableFrames = Collections.unmodifiableList(frameList);
    }

    synchronized List<CallFrameImpl> getCallFrames() {
      if (!scriptsLinkedToFrames) {
        // We expect that ALL the V8 scripts are loaded so we can
        // hook them up to the call frames.
        int frameCount = frameMirrors.length;
        for (int i = 0; i < frameCount; ++i) {
          hookupScriptToFrame(i);
        }
        scriptsLinkedToFrames = true;
      }
      return unmodifableFrames;
    }


    /**
     * Associates a script found in the ScriptManager with the given frame.
     *
     * @param frameIndex to associate a script with
     */
    private void hookupScriptToFrame(int frameIndex) {
      FrameMirror frame = frameMirrors[frameIndex];
      if (frame != null && frame.getScript() == null) {
        Script script = debugSession.getScriptManager().findById(frame.getScriptId());
        if (script != null) {
          frame.setScript(script);
        }
      }
    }
  }

  static InternalContext getInternalContextForTests(DebugContext debugContext) {
    PreContext.UserContext userContext = (PreContext.UserContext) debugContext;
    return userContext.getInternalContextForTests();
  }
}
