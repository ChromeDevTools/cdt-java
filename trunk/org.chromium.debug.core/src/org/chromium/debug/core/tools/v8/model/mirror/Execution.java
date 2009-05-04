// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.model.mirror;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.model.mirror.ValueMirror.PropertyReference;
import org.chromium.debug.core.tools.v8.model.mirror.ValueMirror.Type;
import org.chromium.debug.core.util.JsonUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Matches the execution state of the Chromium V8 engine. The execution state
 * object provides access to the current state of the V8 VM (FrameMirrors).
 *
 * Rips the JSON messages of the backtrace response which contains the current
 * frames.
 */
public class Execution {

  private static final String DEBUGGER_RESERVED = "debugger"; //$NON-NLS-1$

  /** Name format of the frames/frames in Debug view. */
  private static final String DISP_FRAME_NAME_FORMAT = "Javascript [{0}] - "; //$NON-NLS-1$

  private static final String DISP_FUNC_NAME = "UNKNOWN"; //$NON-NLS-1$

  /** Regex for the "text" field of the "backtrace" element response */
  private static final String FRAME_TEXT_REGEX =
      "^#([\\d]+) (.+) ([^\\s]+) line (.+) column (.+)" + //$NON-NLS-1$
          " (?:\\(position (.+)\\))?"; //$NON-NLS-1$

  private static final Pattern FRAME_TEXT_PATTERN =
      Pattern.compile(FRAME_TEXT_REGEX);

  /**
   * The name of the "this" object.
   */
  private static final String THIS_NAME = "this";  //$NON-NLS-1$

  private final ScriptManager scriptManager;

  private final HandleManager handleManager;

  private FrameMirror[] frames;

  private String url = ""; //$NON-NLS-1$

  private final DebugTargetImpl debugTarget;

  public Execution(DebugTargetImpl debugTarget) {
    this.debugTarget = debugTarget;
    this.scriptManager = new ScriptManager(debugTarget);
    this.handleManager = new HandleManager();
  }

  /**
   * Set current frames for this break event.
   *
   * @param framesResp
   *          the "backtrace" V8 reply
   */
  public void setFrames(JSONArray framesResp) {
    this.handleManager.reset();
    int frameCnt = framesResp.size();
    this.frames = new FrameMirror[frameCnt];

    for (int frameIdx = 0; frameIdx < frameCnt; frameIdx++) {
      JSONObject frame = (JSONObject) framesResp.get(frameIdx);

      String frameName = MessageFormat.format(DISP_FRAME_NAME_FORMAT, +frameIdx);
      String frameFunc = DISP_FUNC_NAME;
      String url;

      String text = JsonUtil.getAsString(frame, Protocol.BODY_FRAME_TEXT);
      Matcher m = FRAME_TEXT_PATTERN.matcher(text);
      if (m.matches()) {
        frameFunc = m.group(2);
        url = m.group(3);
      } else {
        ChromiumDebugPlugin.logWarning("Invalid frame text format: {0}", text); //$NON-NLS-1$
        continue;
      }

      int currentLine = JsonUtil.getAsLong(frame, Protocol.BODY_FRAME_LINE).intValue();

      // If we stopped because of the debugger keyword then we're the next
      // line.
      // TODO(apavlov): Terry says: we need to use the Rhino AST to decide
      // if line is debugger keyword. If so find the next sequential line.
      // The below works for simple scripts but doesn't take
      // into account comments, etc.
      String srcLine =
          JsonUtil.getAsString(frame, Protocol.BODY_FRAME_SRCLINE);
      if (srcLine.trim().startsWith(DEBUGGER_RESERVED)) {
        currentLine++;
      }

      frames[frameIdx] =
          new FrameMirror(url, currentLine, frameName, frameFunc);
      // Name we'll show on frame 0 (URL of page).
      if (frameIdx == 0) {
        setUrlName(url);
      }
    }
  }

  public void setUrlName(String urlName) {
    url = urlName;
  }

  public String getUrlName() {
    return url;
  }

  private ValueMirror[] computeLocals(JSONObject frame) {
    JSONObject body = JsonUtil.getAsJSON(frame, Protocol.FRAME_BODY);
    JSONArray args =
        JsonUtil.getAsJSONArray(body, Protocol.BODY_ARGUMENTS);
    JSONArray locals =
        JsonUtil.getAsJSONArray(body, Protocol.BODY_LOCALS);
    JSONArray refs = JsonUtil.getAsJSONArray(frame, Protocol.FRAME_REFS);
    Map<Long, JSONObject> refHandleMap = Protocol.getRefHandleMap(refs);
    handleManager.putAll(refHandleMap);

    int totalLen = args.size() + locals.size() + 1;

    List<ValueMirror> values = new ArrayList<ValueMirror>(totalLen);

    // Receiver ("this")
    Long receiverRef = Protocol.getObjectRef(body, Protocol.FRAME_RECEIVER);
    if (receiverRef != null) {
      JSONObject receiver = handleManager.getHandle(receiverRef);
      values.add(createValueMirror(receiver, THIS_NAME));
    }

    // Arguments
    for (int i = 0; i < args.size(); i++) {
      JSONObject arg = (JSONObject) args.get(i);
      String name = JsonUtil.getAsString(arg, Protocol.ARGUMENT_NAME);
      JSONObject handle = refHandleMap.get(Protocol.getValueRef(arg));
      values.add(createValueMirror(handle, name));
    }

    // Locals
    for (int i = 0; i < locals.size(); i++) {
      JSONObject local = (JSONObject) locals.get(i);
      String localName = JsonUtil.getAsString(local, Protocol.LOCAL_NAME);

      // Chromium can return ".arguments" - ignore
      if (!localName.startsWith(".")) { //$NON-NLS-1$
        JSONObject handle = refHandleMap.get(Protocol.getValueRef(local));
        values.add(createValueMirror(handle, localName));
      }
    }

    return values.toArray(new ValueMirror[values.size()]);
  }

  public static ValueMirror createValueMirror(JSONObject handle, String name) {
    String value = JsonUtil.getAsString(handle, Protocol.REF_TEXT);
    String type = JsonUtil.getAsString(handle, Protocol.REF_TYPE);
    if (Protocol.TYPE_OBJECT.equals(type)) {
      JSONObject protoObj =
          JsonUtil.getAsJSON(handle, Protocol.REF_PROTOOBJECT);
      int parentRef =
          JsonUtil.getAsLong(protoObj, Protocol.REF_PROP_REF).intValue();
      PropertyReference[] obj = Execution.extractObjectProperties(handle);
      ValueMirror mirror =
          new ValueMirror(name, parentRef, obj, JsonUtil.getAsString(
              handle, Protocol.REF_CLASSNAME));
      if (Type.JS_DATE == mirror.getType()) {
        // JS_DATE has a value, unlike other objects
        mirror.setValue(value);
      }
      return mirror;
    } else {
      return new ValueMirror(name, value);
    }
  }

  public static PropertyReference[] extractObjectProperties(JSONObject handle) {
    JSONArray props =
        JsonUtil.getAsJSONArray(handle, Protocol.REF_PROPERTIES);
    int propsLen = props.size();
    List<PropertyReference> objProps =
        new ArrayList<PropertyReference>(propsLen);

    for (int i = 0; i < propsLen; i++) {
      JSONObject prop = (JSONObject) props.get(i);
      int ref =
          JsonUtil.getAsLong(prop, Protocol.REF_PROP_REF).intValue();
      String name = JsonUtil.getAsString(prop, Protocol.REF_PROP_NAME);
      Long propType = JsonUtil.getAsLong(prop, Protocol.REF_PROP_TYPE);

      // Chromium can return ".arguments" - ignore
      if (name.startsWith(".")) { //$NON-NLS-1$
        continue;
      }

      // propType is null if it is e.g. an array element
      int propTypeValue =
          propType != null ? propType.intValue() : PropertyType.NORMAL.value;
      if (propTypeValue == PropertyType.FIELD.value ||
          propTypeValue == PropertyType.CALLBACKS.value ||
          propTypeValue == PropertyType.NORMAL.value) {
        objProps.add(new PropertyReference(ref, name));
      }
    }

    return objProps.toArray(new PropertyReference[objProps.size()]);
  }

  public int setFrameDetails(JSONObject frameResponse) {
    JSONObject body =
        JsonUtil.getAsJSON(frameResponse, Protocol.FRAME_BODY);
    int index = JsonUtil.getAsLong(body, Protocol.BODY_INDEX).intValue();
    FrameMirror frame = getFrame(index);
    frame.setLocals(computeLocals(frameResponse));
    return index;
  }

  public boolean allFramesReady() {
    if (frames == null) {
      return false;
    }
    for (FrameMirror frame : frames) {
      if (!frame.isReady()) {
        return false;
      }
    }

    return true;
  }

  public int getFrameCount() {
    return frames.length;
  }

  public FrameMirror getFrame(int idx) {
    return frames[idx];
  }

  public void hookupScriptToFrame(int frameIndex) {
    FrameMirror frame = getFrame(frameIndex);
    if (frame != null && frame.getScript() == null) {
      Script script =
          getScriptManager().find(frame.getScriptName(), frame.getLine());
      if (script != null) {
        frame.setScript(script);
      }
    }
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }

  public HandleManager getHandleManager() {
    return handleManager;
  }

  public DebugTargetImpl getDebugTarget() {
    return debugTarget;
  }
}
