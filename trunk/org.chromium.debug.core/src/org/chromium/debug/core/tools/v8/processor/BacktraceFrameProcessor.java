// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.processor;

import java.io.IOException;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.V8Command;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.chromium.debug.core.tools.v8.V8ReplyHandler;
import org.chromium.debug.core.tools.v8.model.mirror.Execution;
import org.chromium.debug.core.tools.v8.model.mirror.FrameMirror;
import org.chromium.debug.core.tools.v8.model.mirror.Script;
import org.chromium.debug.core.tools.v8.model.mirror.ScriptManager;
import org.chromium.debug.core.tools.v8.request.V8Request;
import org.chromium.debug.core.util.JsonUtil;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IThread;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the "backtrace" and "frame" V8 command replies.
 */
public class BacktraceFrameProcessor extends V8ReplyHandler {

  private final Execution execution;

  private int lastFrameRequestSeq = -1;

  public BacktraceFrameProcessor(V8DebuggerToolHandler toolHandler) {
    super(toolHandler);
    execution = toolHandler.getExecution();
  }

  @Override
  public void handleReply(JSONObject reply) {
    String commandString = JsonUtil.getAsString(reply, Protocol.KEY_COMMAND);
    V8Command command = V8Command.forString(commandString);

    switch (command) {
      case BACKTRACE:
        JSONObject body = JsonUtil.getAsJSON(reply, Protocol.BACKTRACE_BODY);
        JSONArray jsonFrames = JsonUtil.getAsJSONArray(body, Protocol.BODY_FRAMES);

        execution.setFrames(jsonFrames);

        int framesCount = jsonFrames.size();
        for (int i = 1; i < framesCount; i++) {
          try {
            V8Request request = V8Request.frame(i);
            if (i == framesCount - 1) {
              lastFrameRequestSeq = request.getId();
            }
            getToolHandler().sendV8Command(request.getMessage(), null);
          } catch (IOException e) {
            ChromiumDebugPlugin.log(e);
            return;
          }
          execution.hookupScriptToFrame(i);
        }
        break;
      case FRAME:
        int reqSeq = JsonUtil.getAsLong(reply, Protocol.KEY_REQSEQ).intValue();
        int frameIdx = execution.setFrameDetails(reply);

        if (execution.allFramesReady()) {
          // If script is known associate with this frame
          execution.hookupScriptToFrame(frameIdx);

          // Make sure sources are loaded
          getFrameSources();
        }

        // Last frame received
        if (reqSeq == lastFrameRequestSeq || lastFrameRequestSeq == -1) {
          lastFrameRequestSeq = -1;
          // Tell Eclipse we're suspended and all frames are ready
          int detail = DebugEvent.BREAKPOINT;
          try {
            IThread[] threads = getToolHandler().getDebugTarget().getThreads();
            if (threads.length > 0 && threads[0].isStepping()) {
              detail = DebugEvent.STEP_END;
            }
          } catch (DebugException e) {
            // fall through
          }
          getToolHandler().getDebugTarget().suspended(detail);
        }
        break;
    }

  }

  private void getFrameSources() {
    int framesCnt = execution.getFrameCount();

    for (int i = 0; i < framesCnt; i++) {
      FrameMirror f = execution.getFrame(i);

      final Script script = execution.getScriptManager().find(f.getScriptName(), f.getLine());
      if (script != null && !script.hasSource()) {
        // Source not known for this script block request the source
        // waiting for the response with the request seq sent.
        try {
          getToolHandler().sendV8Command(
              V8Request.source(i, null, null).getMessage(),
              new V8DebuggerToolHandler.MessageReplyCallback() {
                @Override
                public void replyReceived(JSONObject reply) {
                  JSONObject body = JsonUtil.getAsJSON(reply, Protocol.BODY_SOURCE);
                  ScriptManager scriptManager = execution.getScriptManager();
                  scriptManager.setSourceCode(body, script);

                  if (scriptManager.isAllSourcesLoaded()) {
                    int frameCnt = execution.getFrameCount();
                    for (int i = 0; i < frameCnt; i++) {
                      execution.hookupScriptToFrame(i);
                    }
                  }
                }
              });
        } catch (IOException e) {
          ChromiumDebugPlugin.log(e);
          return;
        }
      }
    }
  }

}
