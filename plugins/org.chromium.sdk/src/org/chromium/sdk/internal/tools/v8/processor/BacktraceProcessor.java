// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Script;
import org.chromium.sdk.DebugContext.State;
import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.FrameMirror;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.ScriptManager;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONObject;

/**
 * Handles the "backtrace" V8 command replies.
 */
public class BacktraceProcessor extends V8ResponseCallback {

  public BacktraceProcessor(DebugContextImpl context) {
    super(context);
  }

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
            getDebugContext().setFrames(response);
            loadFrameSources();
          }
        });
        t.setDaemon(true);
        t.start();
        break;
      }
      case EXCEPTION: {
        Thread t = new Thread(new Runnable() {
          public void run() {
            getDebugContext().setState(State.NORMAL);
            getDebugContext().onBreakpointsHit(Collections.<Breakpoint>emptySet());
            getDebugContext().setException(response);
          }
        });
        t.setDaemon(true);
        t.start();
        break;
      }
    }
  }

  protected void loadFrameSources() {
    final DebugContextImpl debugContext = getDebugContext();
    V8DebuggerToolHandler handler = debugContext.getV8Handler();
    int framesCnt = debugContext.getFrameCount();
    Map<Integer, Script> frameToScriptWithoutSources = new HashMap<Integer, Script>();

    for (int i = 0; i < framesCnt; i++) {
      FrameMirror f = debugContext.getFrame(i);
      Script script = debugContext.getScriptManager().findById(f.getScriptId());

      if (script != null && !script.hasSource()) {
        frameToScriptWithoutSources.put(i, script);
      }
    }
    if (frameToScriptWithoutSources.isEmpty()) {
      // All sources are known
      suspend();
      return;
    }
    for (Iterator<Map.Entry<Integer, Script>> it =
             frameToScriptWithoutSources.entrySet().iterator();
         it.hasNext(); ) {
      final Entry<Integer, Script> entry = it.next();
      handler.sendV8Command(
          DebuggerMessageFactory.source(entry.getKey(), null, null),
          new BrowserTabImpl.V8HandlerCallback() {
            public void messageReceived(JSONObject response) {
              JSONObject body = JsonUtil.getBody(response);
              ScriptManager scriptManager = debugContext.getScriptManager();
              scriptManager.setSourceCode(body, entry.getValue());

              if (scriptManager.isAllSourcesLoaded()) {
                int frameCnt = debugContext.getFrameCount();
                for (int i = 0; i < frameCnt; i++) {
                  debugContext.hookupScriptToFrame(i);
                }
                suspend();
              }
            }

            public void failure(String message) {
              getDebugContext().onDebuggerDetached();
            }
          });
    }
  }

  protected void suspend() {
    getDebugContext().getV8Handler().getDebugEventListener().suspended(getDebugContext());
  }

}
