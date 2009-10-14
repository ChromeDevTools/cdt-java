// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.chromium.sdk.internal.ContextBuilder;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.FrameMirror;
import org.chromium.sdk.internal.HandleManager;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the "backtrace" V8 command replies.
 */
class BacktraceProcessor implements org.chromium.sdk.internal.tools.v8.V8CommandProcessor.V8HandlerCallback {

  private final ContextBuilder.ExpectingBacktraceStep step2;

  BacktraceProcessor(ContextBuilder.ExpectingBacktraceStep step2) {
    this.step2 = step2;
 }

  public void messageReceived(final JSONObject response) {
    V8MessageType type = V8MessageType.forString(
        JsonUtil.getAsString(response, V8Protocol.KEY_TYPE));
    if (type != V8MessageType.RESPONSE) {
      handleWrongStacktrace();
    }
    String commandString = JsonUtil.getAsString(response, V8Protocol.KEY_COMMAND);

    DebuggerCommand command = DebuggerCommand.forString(commandString);
    if (command != DebuggerCommand.BACKTRACE) {
      handleWrongStacktrace();
    }

    final DebugContext debugContext = setFrames(response);
    final DebugSession debugSession = step2.getInternalContext().getDebugSession();

    JavascriptVm.ScriptsCallback afterScriptsAreLoaded = new JavascriptVm.ScriptsCallback() {
      public void failure(String errorMessage) {
        handleWrongStacktrace();
      }

      public void success(Collection<Script> scripts) {
        debugSession.getDebugEventListener().suspended(debugContext);
      }
    };

    debugSession.getScriptLoader().loadAllScripts(afterScriptsAreLoaded, null);
  }

  private DebugContext setFrames(JSONObject response) {
    JSONObject body = JsonUtil.getBody(response);
    JSONArray jsonFrames = JsonUtil.getAsJSONArray(body, V8Protocol.BODY_FRAMES);
    int frameCnt = jsonFrames.size();
    FrameMirror[] frameMirrors = new FrameMirror[frameCnt];

    HandleManager handleManager = step2.getInternalContext().getHandleManager();

    JSONArray refs = JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS);
    handleManager.putAll(V8ProtocolUtil.getRefHandleMap(refs));
    for (int frameIdx = 0; frameIdx < frameCnt; frameIdx++) {
      JSONObject frameObject = (JSONObject) jsonFrames.get(frameIdx);
      int index = JsonUtil.getAsLong(frameObject, V8Protocol.BODY_INDEX).intValue();
      JSONObject frame = (JSONObject) jsonFrames.get(frameIdx);
      JSONObject func = JsonUtil.getAsJSON(frame, V8Protocol.FRAME_FUNC);

      String text =
          JsonUtil.getAsString(frame, V8Protocol.BODY_FRAME_TEXT).replace('\r', ' ').replace(
              '\n', ' ');
      Matcher m = FRAME_TEXT_PATTERN.matcher(text);
      m.matches();
      String url = m.group(1);

      int currentLine = JsonUtil.getAsLong(frame, V8Protocol.BODY_FRAME_LINE).intValue();

      // If we stopped because of the debuggerword then we're on the next
      // line.
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
      frameMirrors[index] =
          new FrameMirror(frameObject, url, currentLine, scriptId,
              V8ProtocolUtil.getFunctionName(func));
    }


    return step2.setFrames(frameMirrors);
  }

  public void failure(String message) {
    handleWrongStacktrace();
  }

  private void handleWrongStacktrace() {
    step2.getInternalContext().getContextBuilder().buildSequenceFailure();
  }

  private static final String DEBUGGER_RESERVED = "debugger";

  /** Regex for the "text" field of the "backtrace" element response. */
  private static final String FRAME_TEXT_REGEX =
      "^(?:.+) ([^\\s]+) line (.+) column (.+)" + " (?:\\(position (.+)\\))?";

  /** A pattern for the frame "text" regex. */
  private static final Pattern FRAME_TEXT_PATTERN = Pattern.compile(FRAME_TEXT_REGEX);

}
