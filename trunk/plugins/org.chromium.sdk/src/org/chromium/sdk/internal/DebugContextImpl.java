// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.JavascriptVm.ScriptsCallback;
import org.chromium.sdk.internal.tools.v8.BreakpointManager;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;
import org.chromium.sdk.internal.tools.v8.V8CommandOutput;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor.V8HandlerCallback;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * A default, thread-safe implementation of the JsDebugContext interface.
 */
public class DebugContextImpl implements DebugContext, InternalContext {

  private static final String DEBUGGER_RESERVED = "debugger";

  /** The name of the "this" object to report as a variable name. */
  private static final String THIS_NAME = "this";

  /** The script manager for the associated tab. */
  private final ScriptManager scriptManager;

  /** The handle manager for the associated tab. */
  private final HandleManager handleManager;

  private final V8CommandProcessor v8CommandProcessor;

  /** A helper for performing complex V8-related actions. */
  private final V8Helper v8Helper = new V8Helper(this, THIS_NAME);

  /** The parent JavascriptVm instance. */
  private final JavascriptVmImpl javascriptVmImpl;

  /** The suspension state. */
  private State state;

  /** The JavaScript exception state. */
  private ExceptionData exceptionData;

  /** The context validity token. */
  private ContextToken token;

  /** The context validity token access lock. */
  private final Object tokenAccessLock = new Object();

  /** Context owns breakpoint manager */
  private final BreakpointManager breakpointManager;

  private final ScriptLoader scriptLoader = new ScriptLoader();

  private final Frames frames = new Frames(this);

  private final InternalContext.ContextMessageSender contextMessageSender =
      new InternalContext.ContextMessageSender() {
        public void sendMessageAsync(DebuggerMessage message, boolean isImmediate,
            V8HandlerCallback commandCallback, SyncCallback syncCallback) {
          // TODO(peter.rybin): check state of context
          v8CommandProcessor.sendV8CommandAsync(message, isImmediate,
              commandCallback, syncCallback);
        }
  };

  public DebugContextImpl(JavascriptVmImpl javascriptVmImpl, ProtocolOptions protocolOptions,
      V8CommandOutput v8CommandOutput) {
    createNewToken();
    this.scriptManager = new ScriptManager(protocolOptions);
    this.handleManager = new HandleManager();
    this.javascriptVmImpl = javascriptVmImpl;
    this.breakpointManager = new BreakpointManager(this);
    this.v8CommandProcessor = new V8CommandProcessor(v8CommandOutput, this);
  }

  public JavascriptVmImpl getJavascriptVm() {
    return javascriptVmImpl;
  }

  /**
   * Sets current frames for this break event.
   */
  public void setFrames(FrameMirror[] frameMirrors) {
    frames.setFrames(frameMirrors);
  }

  /**
   * Remembers the current exception state.
   */
  public void setException(ExceptionData exceptionData) {
    setState(State.EXCEPTION);
    frames.setFramesToOneElementArray();

    this.exceptionData = exceptionData;
  }

  public State getState() {
    return state;
  }

  public List<CallFrameImpl> getCallFrames() {
    return frames.getCallFrames();
  }

  public void continueVm(StepAction stepAction, int stepCount, final ContinueCallback callback) {
    if (stepAction == null) {
      throw new NullPointerException();
    }
    synchronized (tokenAccessLock) {
      createNewToken();
      DebuggerMessage message = DebuggerMessageFactory.goOn(
          stepAction, stepCount, getContinueToken());
      // Use non-null commandCallback only if callback is not null
      V8CommandProcessor.V8HandlerCallback commandCallback = callback == null
          ? null
          : new V8CommandProcessor.V8HandlerCallback() {
        public void messageReceived(JSONObject response) {
          if (JsonUtil.isSuccessful(response)) {
            callback.success();
          } else {
            callback.failure(JsonUtil.getAsString(response, V8Protocol.KEY_MESSAGE));
          }
        }

        public void failure(String message) {
          callback.failure(message);
        }
      };
      sendMessageAsync(message, true, commandCallback, null);
    }
  }

  private ContextToken getContinueToken() {
    synchronized (tokenAccessLock) {
      ContextToken theToken;
      if (frames.callFramesCached != null && frames.callFramesCached.size() > 0) {
        theToken = frames.callFramesCached.get(0).getToken();
      } else {
        theToken = getToken();
      }
      return theToken;
    }
  }

  private void createNewToken() {
    synchronized (tokenAccessLock) {
      if (token != null) {
        token.invalidate();
      }
      token = new ContextToken();
    }
  }

  public Collection<Breakpoint> getBreakpointsHit() {
    return breakpointManager.getBreakpointsHit();
  }

  public ExceptionData getExceptionData() {
    return exceptionData;
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }

  public HandleManager getHandleManager() {
    return handleManager;
  }

  public V8CommandProcessor getV8CommandProcessor() {
    return v8CommandProcessor;
  }

  DebugSessionManager getSessionManager() {
    return javascriptVmImpl.getSessionManager();
  }

  public void onDebuggerDetached() {
    getSessionManager().onDebuggerDetached();
    getScriptManager().reset();
    getHandleManager().reset();
    createNewToken();
    frames.callFramesCached = null;
    frames.frameMirrors = null;
  }


  public void loadAllScripts(ScriptsCallback callback) throws MethodIsBlockingException {
    scriptLoader.loadAllScripts(callback);
  }


  /**
   * Sets the current suspension state and performs suspension cleanup.
   *
   * @param state for the current suspension
   */
  public void setState(State state) {
    this.state = state;
    if (state != State.EXCEPTION) {
      exceptionData = null;
    }
    this.handleManager.reset();
  }

  /**
   * Gets all resolved locals for the call frame, caches scripts and objects in
   * the scriptManager and handleManager.
   *
   * @param frame to get the data for
   * @return the mirrors corresponding to the frame locals
   */
  public ValueMirror[] computeLocals(JSONObject frame) {
    return v8Helper.computeLocals(frame);
  }

  public void sendMessageAsync(DebuggerMessage message, boolean isImmediate,
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
   * Gets invoked after a new script has been loaded into the browser tab.
   *
   * @param newScript the newly loaded script
   */
  public void scriptLoaded(Script newScript) {
    getJavascriptVm().getDebugEventListener().scriptLoaded(newScript);
  }

  /**
   * This method MUST NOT be used in short-lived objects that get created on
   * every VM suspension and issue requests dependent on the suspension state
   * of the VM.
   *
   * @return current context token
   */
  public ContextToken getToken() {
    synchronized (tokenAccessLock) {
      return token;
    }
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

  public DebugContextImpl getDebugSession() {
    return this;
  }

  public ContextMessageSender getMessageSender() {
    return contextMessageSender;
  }

  public boolean isValid() {
    return getToken().isValid();
  }

  private static class Frames {

    /** Regex for the "text" field of the "backtrace" element response. */
    private static final String FRAME_TEXT_REGEX =
        "^(?:.+) ([^\\s]+) line (.+) column (.+)" + " (?:\\(position (.+)\\))?";

    /** A pattern for the frame "text" regex. */
    private static final Pattern FRAME_TEXT_PATTERN = Pattern.compile(FRAME_TEXT_REGEX);

    private final DebugContextImpl debugContextImpl;

    /** The frame mirrors while on a breakpoint. */
    private volatile FrameMirror[] frameMirrors;

    /** The cached call frames constructed using frameMirrors. */
    private volatile List<CallFrameImpl> callFramesCached;

    Frames(DebugContextImpl debugContextImpl) {
      this.debugContextImpl = debugContextImpl;
    }

    void setFrames(FrameMirror[] frameMirrors) {
      this.frameMirrors = frameMirrors;
      this.callFramesCached = null;
    }
    void setFramesToOneElementArray() {
      this.frameMirrors = new FrameMirror[1];
      this.callFramesCached = null;
    }

    public List<CallFrameImpl> getCallFrames() {
      synchronized (debugContextImpl.tokenAccessLock) {
        if (callFramesCached == null) {
          // At this point we need to make sure ALL the V8 scripts are loaded so as
          // to hook them up to the call frames.
          debugContextImpl.scriptLoader.loadAllScripts(null);
          int frameCount = getFrameCount();
          List<CallFrameImpl> frames = new ArrayList<CallFrameImpl>(frameCount);
          ContextToken theToken = debugContextImpl.getToken();
          for (int i = 0; i < frameCount; ++i) {
            frames.add(new CallFrameImpl(getFrame(i), i, debugContextImpl, theToken));
            hookupScriptToFrame(i);
          }
          callFramesCached = Collections.unmodifiableList(frames);
        }
        return callFramesCached;
      }
    }


    /**
     * @return count of frames in the current stack
     */
    public int getFrameCount() {
      return frameMirrors.length;
    }

    /**
     * @param index of the frame
     * @return a FrameMirror instance for the specified frame index
     */
    public FrameMirror getFrame(int index) {
      return frameMirrors[index];
    }

    /**
     * Associates a script found in the ScriptManager with the given frame.
     *
     * @param frameIndex to associate a script with
     */
    public void hookupScriptToFrame(int frameIndex) {
      FrameMirror frame = getFrame(frameIndex);
      if (frame != null && frame.getScript() == null) {
        Script script = debugContextImpl.getScriptManager().findById(frame.getScriptId());
        if (script != null) {
          frame.setScript(script);
        }
      }
    }
  }

  private class ScriptLoader {

    /** Whether the initial script loading has completed. */
    private volatile boolean doneInitialScriptLoad = false;

    /**
     * Loads all scripts from the remote if necessary, and feeds them into the
     * callback provided (if any).
     *
     * @param callback nullable callback to invoke when the scripts are ready
     */
    void loadAllScripts(final ScriptsCallback callback) throws MethodIsBlockingException {
      if (!isDoneInitialScriptLoad()) {
        setDoneInitialScriptLoad(true);
        // Not loaded the scripts initially, do full load.
        v8Helper.reloadAllScripts(new V8HandlerCallback() {
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
        });
      } else {
        if (callback != null) {
          callback.success(getScriptManager().allScripts());
        }
      }
    }

    private boolean isDoneInitialScriptLoad() {
      return doneInitialScriptLoad;
    }

    private void setDoneInitialScriptLoad(boolean doneInitialScriptLoad) {
      this.doneInitialScriptLoad = doneInitialScriptLoad;
    }
  }
}
