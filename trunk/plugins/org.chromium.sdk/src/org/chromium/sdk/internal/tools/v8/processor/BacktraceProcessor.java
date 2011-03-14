// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.chromium.sdk.internal.ContextBuilder;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.FrameMirror;
import org.chromium.sdk.internal.HandleManager;
import org.chromium.sdk.internal.protocol.BacktraceCommandBody;
import org.chromium.sdk.internal.protocol.CommandResponse;
import org.chromium.sdk.internal.protocol.FrameObject;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.protocol.data.ScriptHandle;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.json.simple.JSONObject;

/**
 * Handles the "backtrace" V8 command replies.
 */
class BacktraceProcessor implements V8CommandProcessor.V8HandlerCallback {

  private final ContextBuilder.ExpectingBacktraceStep step2;

  BacktraceProcessor(ContextBuilder.ExpectingBacktraceStep step2) {
    this.step2 = step2;
 }

  public void messageReceived(CommandResponse response) {
    String commandString = response.getCommand();

    DebuggerCommand command = DebuggerCommand.forString(commandString);
    if (command != DebuggerCommand.BACKTRACE) {
      handleWrongStacktrace();
    }
    SuccessCommandResponse successResponse = response.asSuccess();
    if (successResponse == null) {
      handleWrongStacktrace();
    }

    final DebugContext debugContext = setFrames(successResponse);
    final DebugSession debugSession = step2.getInternalContext().getDebugSession();

    JavascriptVm.ScriptsCallback afterScriptsAreLoaded = new JavascriptVm.ScriptsCallback() {
      public void failure(String errorMessage) {
        handleWrongStacktrace();
      }

      public void success(Collection<Script> scripts) {
        debugSession.getDebugEventListener().suspended(debugContext);
      }
    };

    debugSession.getScriptManagerProxy().getAllScripts(afterScriptsAreLoaded, null);
  }

  private DebugContext setFrames(SuccessCommandResponse response) {
    BacktraceCommandBody body;
    try {
      body = response.getBody().asBacktraceCommandBody();
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }
    List<FrameObject> jsonFrames = body.getFrames();
    if (jsonFrames == null) {
      jsonFrames = Collections.emptyList();
    }
    int frameCnt = jsonFrames.size();
    FrameMirror[] frameMirrors = new FrameMirror[frameCnt];

    HandleManager handleManager = step2.getInternalContext().getHandleManager();

    List<SomeHandle> refs = response.getRefs();
    handleManager.putAll(refs);
    for (int frameIdx = 0; frameIdx < frameCnt; frameIdx++) {
      FrameObject frameObject = jsonFrames.get(frameIdx);
      int index = (int) frameObject.getIndex();
      FrameObject frame = jsonFrames.get(frameIdx);
      JSONObject func = frame.getFunc();

      int currentLine = (int) frame.getLine();

      // If we stopped because of the debuggerword then we're on the next
      // line.
      // TODO(apavlov): Terry says: we need to use the [e.g. Rhino] AST to
      // decide if line is debuggerword. If so, find the next sequential line.
      // The below works for simple scripts but doesn't take into account
      // comments, etc.
      String srcLine = frame.getSourceLineText();
      if (srcLine.trim().startsWith(DEBUGGER_RESERVED)) {
        currentLine++;
      }
      Long scriptRef = V8ProtocolUtil.getObjectRef(frame.getScript());

      Long scriptId = -1L;
      if (scriptRef != null) {
        SomeHandle handle = handleManager.getHandle(scriptRef);
        ScriptHandle scriptHandle;
        try {
          scriptHandle = handle.asScriptHandle();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }
        if (handle != null) {
          scriptId = scriptHandle.id();
        }
      }
      frameMirrors[index] =
          new FrameMirror(frameObject, currentLine, scriptId,
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
}
