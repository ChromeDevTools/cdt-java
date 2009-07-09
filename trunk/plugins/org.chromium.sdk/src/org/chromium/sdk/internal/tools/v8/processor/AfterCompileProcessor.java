// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.Collections;

import org.chromium.sdk.Script;
import org.chromium.sdk.internal.CountingLock;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.BrowserTabImpl.V8HandlerCallback;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Listens for scripts sent in the "afterCompile" events and requests their
 * sources.
 */
public class AfterCompileProcessor extends V8ResponseCallback {

  private final CountingLock completionLock = new CountingLock();

  public AfterCompileProcessor(DebugContextImpl context) {
    super(context);
  }

  public void messageReceived(JSONObject response) {
    if (!JsonUtil.isSuccessful(response)) {
      return;
    }
    JSONObject script = getScriptToLoad(response);
    if (script == null) {
      return;
    }
    final DebugContextImpl debugContext = getDebugContext();
    lock();
    debugContext.getV8Handler().sendV8Command(
        DebuggerMessageFactory.scripts(
            Collections.singletonList(V8ProtocolUtil.getScriptIdFromResponse(script)), true),
        new V8HandlerCallback(){
          public void messageReceived(JSONObject response) {
            try {
              if (!JsonUtil.isSuccessful(response)) {
                return;
              }
              JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
              // body is an array of scripts
              if (body.size() == 0) {
                return; // The script did not arrive (bad id?)
              }
              debugContext.getScriptManager().addScript(
                  (JSONObject) body.get(0),
                  JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS));
            } finally {
              unlock();
            }
          }

          public void failure(String message) {
            unlock();
          }
        });
    debugContext.evaluateJavascript();
  }

  /**
   * Waits until all the requested scripts have been loaded.
   */
  public void awaitScripts() {
    completionLock.await();
  }

  protected void unlock() {
    completionLock.unlock();
  }

  protected void lock() {
    completionLock.lock();
  }

  private static JSONObject getScriptToLoad(JSONObject response) {
    JSONObject script = JsonUtil.getAsJSON(JsonUtil.getBody(response), V8Protocol.FRAME_SCRIPT);
    if (DebugContextImpl.JAVASCRIPT_VOID.equals(JsonUtil.getAsString(script, "sourceStart")) ||
        script.get(V8Protocol.CONTEXT) != null ||
        V8ProtocolUtil.getScriptType(JsonUtil.getAsLong(script, V8Protocol.BODY_SCRIPT_TYPE)) ==
            Script.Type.NATIVE) {
      return null;
    }
    return V8ProtocolUtil.scriptWithName(
        script, JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS));
  }

}
