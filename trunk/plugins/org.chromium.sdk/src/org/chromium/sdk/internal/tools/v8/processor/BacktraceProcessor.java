// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chromium.sdk.internal.DebugContextImpl;
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
public class BacktraceProcessor extends V8ResponseCallback {

  public BacktraceProcessor(DebugContextImpl context) {
    super(context);
  }

  @Override
  public void messageReceived(final JSONObject response) {
    V8MessageType type = V8MessageType.forString(
        JsonUtil.getAsString(response, V8Protocol.KEY_TYPE));
    String commandString = JsonUtil.getAsString(response, V8MessageType.RESPONSE == type
        ? V8Protocol.KEY_COMMAND
        : V8Protocol.KEY_EVENT);
    DebuggerCommand command = DebuggerCommand.forString(commandString);

    switch (command) {
      case BACKTRACE: {
        Thread t = new Thread(new Runnable() {
          public void run() {
            setFrames(getDebugContext(), response);
            suspend();
          }
        });
        t.setDaemon(true);
        t.start();
        break;
      }
    }
  }

  protected void suspend() {
    getDebugContext().getDebugEventListener().suspended(getDebugContext());
  }

  private void setFrames(DebugContextImpl debugContextImpl, JSONObject response) {
    JSONObject body = JsonUtil.getBody(response);
    JSONArray jsonFrames = JsonUtil.getAsJSONArray(body, V8Protocol.BODY_FRAMES);
    int frameCnt = jsonFrames.size();
    FrameMirror[] frameMirrors = new FrameMirror[frameCnt];

    HandleManager handleManager = debugContextImpl.getHandleManager();

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
          new FrameMirror(debugContextImpl, frameObject, url, currentLine, scriptId, V8ProtocolUtil
              .getFunctionName(func));
    }


    debugContextImpl.setFrames(frameMirrors);
  }

  private static final String DEBUGGER_RESERVED = "debugger";

  /** Regex for the "text" field of the "backtrace" element response. */
  private static final String FRAME_TEXT_REGEX =
      "^(?:.+) ([^\\s]+) line (.+) column (.+)" + " (?:\\(position (.+)\\))?";

  /** A pattern for the frame "text" regex. */
  private static final Pattern FRAME_TEXT_PATTERN = Pattern.compile(FRAME_TEXT_REGEX);

}
