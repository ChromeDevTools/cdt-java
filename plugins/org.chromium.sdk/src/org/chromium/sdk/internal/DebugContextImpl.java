// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JsDataType;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.Script;
import org.chromium.sdk.internal.BrowserTabImpl.V8HandlerCallback;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.ScriptsMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A default, thread-safe implementation of the JsDebugContext interface.
 */
public class DebugContextImpl implements DebugContext {

  private static final EnumSet<PropertyType> VISIBLE_PROPERTY_TYPES =
      EnumSet.of(
          PropertyType.NORMAL,
          PropertyType.FIELD,
          PropertyType.CALLBACKS,
          PropertyType.INTERCEPTOR);

  /**
   * A no-op javascript to evaluate.
   */
  private static final String JAVASCRIPT_VOID = "javascript:void(0);";

  private static final String DEBUGGER_RESERVED = "debugger";

  /** Regex for the "text" field of the "backtrace" element response. */
  private static final String FRAME_TEXT_REGEX =
      "^#([\\d]+) (.+) ([^\\s]+) line (.+) column (.+)" + " (?:\\(position (.+)\\))?";

  private static final Pattern FRAME_TEXT_PATTERN = Pattern.compile(FRAME_TEXT_REGEX);

  /** The name of the "this" object to report as a variable name. */
  private static final String THIS_NAME = "this";

  /** The name of the "exception" object to report as a variable name. */
  private static final String EXCEPTION_NAME = "exception";

  /** The script manager for the associated tab. */
  private final ScriptManager scriptManager;

  /** The handle manager for the associated tab. */
  private final HandleManager handleManager;

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

  /** The Javascript exception state. */
  private ExceptionData exceptionData;

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
      Long funcRef = V8ProtocolUtil.getObjectRef(frame, V8Protocol.FRAME_FUNC);

      String text = JsonUtil.getAsString(frame, V8Protocol.BODY_FRAME_TEXT);
      Matcher m = FRAME_TEXT_PATTERN.matcher(text);
      m.matches();
      String url = m.group(3);

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

      ValueMirror[] locals = computeLocals(frameObject);
      JSONObject func = handleManager.getHandle(funcRef);
      Long scriptId = -1L;
      if (scriptRef != null) {
        JSONObject handle = handleManager.getHandle(scriptRef);
        if (handle != null) {
          scriptId = JsonUtil.getAsLong(handle, V8Protocol.ID);
        }
      }
      frameMirrors[index] =
          new FrameMirror(url, currentLine, scriptId, getFunctionName(func), locals);
    }
  }

  /**
   * Remembers the current exception state and requests a backtrace in a
   * non-blocking way.
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
            createValueMirror(exception, EXCEPTION_NAME),
            JsonUtil.getAsBoolean(body, V8Protocol.UNCAUGHT),
            sourceText,
            JsonUtil.getAsString(exception,
            V8Protocol.REF_TEXT));
  }

  /**
   * Constructs a ValueMirror given a V8 debugger object specification and the
   * value name.
   *
   * @param handle containing the object specification from the V8 debugger
   * @param name of the value to construct
   * @return a ValueMirror instance with the specified name, containing data
   *         from handle
   */
  public static ValueMirror createValueMirror(JSONObject handle, String name) {
    String value = JsonUtil.getAsString(handle, V8Protocol.REF_TEXT);
    String typeString = JsonUtil.getAsString(handle, V8Protocol.REF_TYPE);
    String className = JsonUtil.getAsString(handle, V8Protocol.REF_CLASSNAME);
    JsDataType type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (JsDataType.isObjectType(type)) {
      JSONObject protoObj = JsonUtil.getAsJSON(handle, V8Protocol.REF_PROTOOBJECT);
      int parentRef = JsonUtil.getAsLong(protoObj, V8Protocol.REF).intValue();
      PropertyReference[] propertyRefs = DebugContextImpl.extractObjectProperties(handle);
      return new ValueMirror(name, parentRef, propertyRefs, className);
    } else {
      return new ValueMirror(name, value, type);
    }
  }

  /**
   * Constructs {@code PropertyReference}s from the specified object.
   *
   * @param handle containing the object specification from the V8 debugger
   * @return {@code PropertyReference}s based on the {@code handle} data
   */
  public static PropertyReference[] extractObjectProperties(JSONObject handle) {
    JSONArray props = JsonUtil.getAsJSONArray(handle, V8Protocol.REF_PROPERTIES);
    int propsLen = props.size();
    List<PropertyReference> objProps = new ArrayList<PropertyReference>(propsLen);

    for (int i = 0; i < propsLen; i++) {
      JSONObject prop = (JSONObject) props.get(i);
      int ref = JsonUtil.getAsLong(prop, V8Protocol.REF).intValue();
      String name = JsonUtil.getAsString(prop, V8Protocol.REF_PROP_NAME);
      if (name == null) {
        name = String.valueOf(JsonUtil.getAsLong(prop, V8Protocol.REF_PROP_NAME));
      }
      Long propType = JsonUtil.getAsLong(prop, V8Protocol.REF_PROP_TYPE);

      if (isInternalProperty(name)) {
        continue;
      }

      // propType is NORMAL by default
      int propTypeValue = propType != null
          ? propType.intValue()
          : PropertyType.NORMAL.value;

      PropertyType type = PropertyType.forValue(propTypeValue);
      if (VISIBLE_PROPERTY_TYPES.contains(type)) {
        objProps.add(new PropertyReference(ref, name));
      }
    }

    return objProps.toArray(new PropertyReference[objProps.size()]);
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

  @Override
  public State getState() {
    return state;
  }

  @Override
  public JsStackFrameImpl[] getStackFrames() {
    if (stackFramesCached == null) {
      int frameCount = getFrameCount();
      stackFramesCached = new JsStackFrameImpl[frameCount];
      for (int i = 0; i < frameCount; ++i) {
        stackFramesCached[i] = new JsStackFrameImpl(getFrame(i), i, this);
        hookupScriptToFrame(i);
      }
    }
    return stackFramesCached;
  }

  @Override
  public void continueVm(StepAction stepAction, int stepCount, final ContinueCallback callback) {
    DebuggerMessage message = DebuggerMessageFactory.goOn(stepAction, stepCount);
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

  @Override
  public Collection<Breakpoint> getBreakpointsHit() {
    return breakpointsHit != null
        ? breakpointsHit
        : Collections.<Breakpoint> emptySet();
  }

  @Override
  public ExceptionData getExceptionData() {
    return exceptionData;
  }

  private Exception sendMessage(boolean isSync, DebuggerMessage message,
      BrowserTabImpl.V8HandlerCallback commandCallback) {
    if (isSync) {
      return handler.sendV8CommandBlocking(message, false, commandCallback);
    } else {
      handler.sendV8Command(message, commandCallback);
      return null;
    }
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
    handler.onDebuggerDetached();
    scriptManager.reset();
    handleManager.reset();
    stackFramesCached = null;
    frameMirrors = null;
  }

  public void onBreakpointsHit(Collection<Breakpoint> breakpointsHit) {
    this.breakpointsHit = Collections.unmodifiableCollection(breakpointsHit);
  }

  /**
   * Clears the scripts cache and reloads all scripts from the remote.
   *
   * @param callback to invoke when the scripts are ready
   */
  public void reloadAllScripts(V8HandlerCallback callback) {
    getScriptManager().reset();
    getV8Handler().sendV8Command(
        DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true), callback);
    evaluateJavascript();
  }

  /**
   * Evaluates a javascript snippet to pump the debugger command queue.
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
  private ValueMirror[] computeLocals(JSONObject frame) {
    JSONArray args = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_ARGUMENTS);
    JSONArray locals = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_LOCALS);

    int maxLookups = args.size() + locals.size() + 3 /* "this", script, function */;

    final List<ValueMirror> values = new ArrayList<ValueMirror>(maxLookups);
    final Map<Long, String> refToName = new HashMap<Long, String>();

    // Frame script
    Long scriptRef = V8ProtocolUtil.getObjectRef(frame, V8Protocol.FRAME_SCRIPT);
    if (scriptRef != null) {
      JSONObject scriptObject = handleManager.getHandle(scriptRef);
      if (scriptObject == null) {
        refToName.put(scriptRef, null);
      } else {
        scriptManager.addScript(scriptObject);
      }
    }

    // Frame function
    Long funcRef = V8ProtocolUtil.getObjectRef(frame, V8Protocol.FRAME_FUNC);
    if (funcRef != null) {
      JSONObject funcObject = handleManager.getHandle(funcRef);
      if (funcObject == null) {
        refToName.put(funcRef, null);
      }
    }

    // Receiver ("this")
    Long receiverRef = V8ProtocolUtil.getObjectRef(frame, V8Protocol.FRAME_RECEIVER);
    if (receiverRef != null) {
      JSONObject receiver = handleManager.getHandle(receiverRef);
      if (receiver != null) {
        values.add(createValueMirror(receiver, THIS_NAME));
      } else {
        refToName.put(receiverRef, THIS_NAME);
      }
    }

    // Arguments
    for (int i = 0; i < args.size(); i++) {
      JSONObject arg = (JSONObject) args.get(i);
      String name = JsonUtil.getAsString(arg, V8Protocol.ARGUMENT_NAME);
      if (name == null) {
        // an unnamed actual argument (there is no formal counterpart in the
        // method signature) that will be available in the "arguments" object
        continue;
      }
      Long ref = V8ProtocolUtil.getValueRef(arg);
      JSONObject handle = handleManager.getHandle(ref);
      if (handle == null) {
        refToName.put(ref, name);
      } else {
        values.add(createValueMirror(handle, name));
      }
    }

    // Locals
    for (int i = 0; i < locals.size(); i++) {
      JSONObject local = (JSONObject) locals.get(i);
      String localName = JsonUtil.getAsString(local, V8Protocol.LOCAL_NAME);

      if (!isInternalProperty(localName)) {
        Long ref = V8ProtocolUtil.getValueRef(local);
        JSONObject handle = handleManager.getHandle(ref);
        if (handle == null) {
          refToName.put(ref, localName);
        } else {
          values.add(createValueMirror(handle, localName));
        }
      }
    }

    if (!refToName.isEmpty()) {
      handler.sendV8CommandBlocking(DebuggerMessageFactory.lookup(
          new ArrayList<Long>(refToName.keySet())),
          true,
          new BrowserTabImpl.V8HandlerCallback() {

            @Override
            public void messageReceived(JSONObject response) {
              if (!JsonUtil.isSuccessful(response)) {
                return;
              }
              processLookupResponse(values, refToName, JsonUtil.getBody(response));
            }

            @Override
            public void failure(String message) {
              // do nothing, failures will occur later
            }
          });
    }

    return values.toArray(new ValueMirror[values.size()]);
  }

  protected void processLookupResponse(final List<ValueMirror> values,
      final Map<Long, String> refToName, JSONObject body) {
    for (Map.Entry<Long, String> entry : refToName.entrySet()) {
      Long ref = entry.getKey();
      JSONObject object = JsonUtil.getAsJSON(body, String.valueOf(ref));
      if (object != null) {
        handleManager.put(ref, object);
        String name = entry.getValue();
        // name is null for objects that should not be put into handleManager
        if (name != null) {
          ValueMirror mirror = createValueMirror(object, name);
          if (THIS_NAME.equals(name)) {
            // "this" should go first
            values.add(0, mirror);
          } else {
            values.add(mirror);
          }
        } else {
          scriptManager.addScript(object); // might be a script object
        }
      }
    }
  }

  private static String getFunctionName(JSONObject func) {
    if (func == null) {
      return "<unknown>";
    } else {
      String name = getNameOrInferred(func, V8Protocol.LOCAL_NAME);
      if (name == null || name.isEmpty()) {
        return "(anonymous function)";
      } else {
        return name;
      }
    }
  }

  private static String getNameOrInferred(JSONObject obj, V8Protocol nameProperty) {
    String name = JsonUtil.getAsString(obj, nameProperty);
    if (name == null || name.isEmpty()) {
      name = JsonUtil.getAsString(obj, V8Protocol.INFERRED_NAME);
    }
    return name;
  }

  private static boolean isInternalProperty(String propertyName) {
    // Chromium can return properties like ".arguments". They should be ignored.
    return propertyName.startsWith(".");
  }
}
