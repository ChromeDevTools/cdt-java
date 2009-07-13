// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.Script;
import org.chromium.sdk.BrowserTab.ScriptsCallback;
import org.chromium.sdk.internal.BrowserTabImpl.V8HandlerCallback;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A default, thread-safe implementation of the JsDebugContext interface.
 */
public class DebugContextImpl implements DebugContext {

  /**
   * A no-op JavaScript to evaluate.
   */
  public static final String JAVASCRIPT_VOID = "javascript:void(0);";

  private static final String DEBUGGER_RESERVED = "debugger";

  /** Regex for the "text" field of the "backtrace" element response. */
  private static final String FRAME_TEXT_REGEX =
      "^(?:.+) ([^\\s]+) line (.+) column (.+)" + " (?:\\(position (.+)\\))?";

  /** A pattern for the frame "text" regex. */
  private static final Pattern FRAME_TEXT_PATTERN = Pattern.compile(FRAME_TEXT_REGEX);

  /** The name of the "this" object to report as a variable name. */
  private static final String THIS_NAME = "this";

  /** The name of the "exception" object to report as a variable name. */
  private static final String EXCEPTION_NAME = "exception";

  /** The script manager for the associated tab. */
  private final ScriptManager scriptManager;

  /** The handle manager for the associated tab. */
  private final HandleManager handleManager;

  /** A helper for performing complex V8-related actions. */
  private final V8Helper v8Helper = new V8Helper(this, THIS_NAME);

  /**
   * The V8 debugger tool handler for the associated tab (used for sending
   * messages).
   */
  private final V8DebuggerToolHandler handler;

  /** The parent BrowserImpl instance. */
  private final BrowserTabImpl browserTabImpl;

  /** The frame mirrors while on a breakpoint. */
  private volatile FrameMirror[] frameMirrors;

  /** The cached stack frames constructed using frameMirrors. */
  private volatile JsStackFrameImpl[] stackFramesCached;

  /** The breakpoints hit before suspending. */
  private volatile Collection<Breakpoint> breakpointsHit;

  /** The suspension state. */
  private State state;

  /** The JavaScript exception state. */
  private ExceptionData exceptionData;

  /** Whether the initial script loading has completed. */
  private volatile boolean doneInitialScriptLoad = false;

  public DebugContextImpl(BrowserTabImpl browserTabImpl) {
    this.scriptManager = new ScriptManager();
    this.handleManager = new HandleManager();
    this.browserTabImpl = browserTabImpl;
    this.handler = new V8DebuggerToolHandler(browserTabImpl.getBrowser(), this);
  }

  public BrowserTabImpl getTab() {
    return browserTabImpl;
  }

  public int getTabId() {
    return browserTabImpl.getId();
  }

  /**
   * Sets current frames for this break event.
   * <p>
   * WARNING. Performs debugger commands in a blocking way.
   *
   * @param response the "backtrace" V8 reply
   */
  public void setFrames(JSONObject response) {
    JSONObject body = JsonUtil.getBody(response);
    JSONArray jsonFrames = JsonUtil.getAsJSONArray(body, V8Protocol.BODY_FRAMES);
    int frameCnt = jsonFrames.size();
    this.frameMirrors = new FrameMirror[frameCnt];
    this.stackFramesCached = null;

    JSONArray refs = JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS);
    handleManager.putAll(V8ProtocolUtil.getRefHandleMap(refs));
    for (int frameIdx = 0; frameIdx < frameCnt; frameIdx++) {
      JSONObject frameObject = (JSONObject) jsonFrames.get(frameIdx);
      int index = JsonUtil.getAsLong(frameObject, V8Protocol.BODY_INDEX).intValue();
      JSONObject frame = (JSONObject) jsonFrames.get(frameIdx);
      JSONObject func = JsonUtil.getAsJSON(frame, V8Protocol.FRAME_FUNC);

      String text = JsonUtil.getAsString(frame, V8Protocol.BODY_FRAME_TEXT)
          .replace('\r', ' ').replace('\n', ' ');
      Matcher m = FRAME_TEXT_PATTERN.matcher(text);
      m.matches();
      String url = m.group(1);

      int currentLine = JsonUtil.getAsLong(frame, V8Protocol.BODY_FRAME_LINE).intValue();

      // If we stopped because of the debuggerword then we're on the next line.
      // TODO(apavlov): Terry says: we need to use the [e.g. Rhino] AST to
      // decide if line is debuggerword. If so, find the next sequential line.
      // The below works for simple scripts but doesn't take into account
      // comments, etc.
      String srcLine = JsonUtil.getAsString(frame, V8Protocol.BODY_FRAME_SRCLINE);
      if (srcLine.trim().startsWith(DEBUGGER_RESERVED)) {
        currentLine++;
      }
      Long scriptRef = V8ProtocolUtil.getObjectRef(frame, V8Protocol.FRAME_SCRIPT);

      Long scriptId = -1L;
      if (scriptRef != null) {
        JSONObject handle = handleManager.getHandle(scriptRef);
        if (handle != null) {
          scriptId = JsonUtil.getAsLong(handle, V8Protocol.ID);
        }
      }
      frameMirrors[index] = new FrameMirror(
          this, frameObject, url, currentLine, scriptId,
          V8ProtocolUtil.getFunctionName(func));
    }
  }

  /**
   * Remembers the current exception state.
   *
   * @param response the "exception" event V8 JSON message
   */
  public void setException(JSONObject response) {
    setState(State.EXCEPTION);
    JSONObject body = JsonUtil.getBody(response);
    this.frameMirrors = new FrameMirror[1];
    this.stackFramesCached = null;

    JSONArray refs = JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS);
    JSONObject exception = JsonUtil.getAsJSON(body, V8Protocol.EXCEPTION);
    Map<Long, JSONObject> refHandleMap = V8ProtocolUtil.getRefHandleMap(refs);
    V8ProtocolUtil.putHandle(refHandleMap, exception);
    handleManager.putAll(refHandleMap);

    // source column is not exposed ("sourceColumn" in "body")
    String sourceText = JsonUtil.getAsString(body, V8Protocol.BODY_FRAME_SRCLINE);

    this.exceptionData =
        new ExceptionDataImpl(this,
            V8Helper.createValueMirror(exception, EXCEPTION_NAME),
            JsonUtil.getAsBoolean(body, V8Protocol.UNCAUGHT),
            sourceText,
            JsonUtil.getAsString(exception, V8Protocol.REF_TEXT));
  }

  public State getState() {
    return state;
  }

  public JsStackFrameImpl[] getStackFrames() {
    if (stackFramesCached == null) {
      // At this point we need to make sure ALL the V8 scripts are loaded so as
      // to hook them up to the stack frames.
      loadAllScripts(null);
      int frameCount = getFrameCount();
      stackFramesCached = new JsStackFrameImpl[frameCount];
      for (int i = 0; i < frameCount; ++i) {
        stackFramesCached[i] = new JsStackFrameImpl(getFrame(i), i, this);
        hookupScriptToFrame(i);
      }
    }
    return stackFramesCached;
  }

  public void continueVm(StepAction stepAction, int stepCount, final ContinueCallback callback) {
    DebuggerMessage message = DebuggerMessageFactory.goOn(stepAction, stepCount, null);
    // Use non-null commandCallback only if callback is not null
    BrowserTabImpl.V8HandlerCallback commandCallback = callback == null
        ? null
        : new BrowserTabImpl.V8HandlerCallback() {
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
    sendMessage(false, message, commandCallback);
  }

  public Collection<Breakpoint> getBreakpointsHit() {
    return breakpointsHit != null
        ? breakpointsHit
        : Collections.<Breakpoint> emptySet();
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

  public V8DebuggerToolHandler getV8Handler() {
    return handler;
  }

  public void onDebuggerDetached() {
    getV8Handler().onDebuggerDetached();
    getScriptManager().reset();
    getHandleManager().reset();
    this.stackFramesCached = null;
    this.frameMirrors = null;
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
      Script script = getScriptManager().findById(frame.getScriptId());
      if (script != null) {
        frame.setScript(script);
      }
    }
  }

  /**
   * Stores the breakpoints associated with V8 suspension event (empty if an
   * exception or a step end).
   *
   * @param breakpointsHit the breakpoints that were hit
   */
  public void onBreakpointsHit(Collection<Breakpoint> breakpointsHit) {
    this.breakpointsHit = Collections.unmodifiableCollection(breakpointsHit);
  }

  /**
   * Loads all scripts from the remote if necessary, and feeds them into the
   * callback provided (if any).
   *
   * @param callback nullable callback to invoke when the scripts are ready
   */
  public void loadAllScripts(final ScriptsCallback callback) {
    if (!doneInitialScriptLoad) {
      // Not loaded the scripts initially, do full load.
      v8Helper.reloadAllScripts(new V8HandlerCallback() {
        public void messageReceived(JSONObject response) {
          doneInitialScriptLoad = true;
          if (callback != null) {
            if (JsonUtil.isSuccessful(response)) {
              callback.success(getScriptManager().allScripts());
            } else {
              callback.failure(JsonUtil.getAsString(response, V8Protocol.KEY_MESSAGE));
            }
          }
        }

        public void failure(String message) {
          doneInitialScriptLoad = true;
          if (callback != null) {
            callback.failure(message);
          }
        }
      });
    } else {
      // Just wait for the scripts from afterCompile to load (if any).
      getV8Handler().awaitScripts();
      if (callback != null) {
        callback.success(getScriptManager().allScripts());
      }
    }
  }

  /**
   * Evaluates a JavaScript snippet to pump the debugger command queue.
   */
  public void evaluateJavascript() {
    getV8Handler().sendEvaluateJavascript(JAVASCRIPT_VOID);
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
   * Gets all resolved locals for the stack frame, caches scripts and objects in
   * the scriptManager and handleManager.
   *
   * @param frame to get the data for
   * @return the mirrors corresponding to the frame locals
   */
  public ValueMirror[] computeLocals(JSONObject frame) {
    return v8Helper.computeLocals(frame);
  }

  private Exception sendMessage(boolean isSync, DebuggerMessage message,
      BrowserTabImpl.V8HandlerCallback commandCallback) {
    if (isSync) {
      return getV8Handler().sendV8CommandBlocking(message, commandCallback);
    } else {
      getV8Handler().sendV8Command(message, commandCallback);
      return null;
    }
  }

  /**
   * Gets invoked when a navigation event is reported by the browser tab.
   *
   * @param newUrl the new URL of the tab being debugged
   */
  public void navigated(String newUrl) {
    doneInitialScriptLoad = false; // we should forget all our scripts
    getTab().getDebugEventListener().navigated(newUrl);
  }

}
