// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import org.chromium.sdk.Script;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.ProtocolOptions;
import org.chromium.sdk.internal.DebugContextImpl.SendingType;
import org.chromium.sdk.internal.tools.v8.ChromeDevToolSessionManager;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collections;

/**
 * Listens for scripts sent in the "afterCompile" events and requests their
 * sources.
 */
public class AfterCompileProcessor extends V8ResponseCallback {

  public AfterCompileProcessor(DebugContextImpl context) {
    super(context);
  }

  public void messageReceived(JSONObject response) {
    if (!JsonUtil.isSuccessful(response)) {
      return;
    }
    final DebugContextImpl debugContext = getDebugContext();
    JSONObject script = getScriptToLoad(response,
        debugContext.getScriptManager().getProtocolOptions());
    if (script == null) {
      return;
    }
    debugContext.sendMessage(
        SendingType.ASYNC_IMMEDIATE,
        DebuggerMessageFactory.scripts(
            Collections.singletonList(V8ProtocolUtil.getScriptIdFromResponse(script)), true),
        new V8CommandProcessor.V8HandlerCallback(){
          public void messageReceived(JSONObject response) {
            if (!JsonUtil.isSuccessful(response)) {
              return;
            }
            JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
            // body is an array of scripts
            if (body.size() == 0) {
              return; // The script did not arrive (bad id?)
            }
            Script newScript = debugContext.getScriptManager().addScript(
                (JSONObject) body.get(0),
                JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS));
            if (newScript != null) {
              getDebugContext().scriptLoaded(newScript);
            }
          }

          public void failure(String message) {
            // The script is now missing.
          }
        });
  }

  private static JSONObject getScriptToLoad(JSONObject response, ProtocolOptions protocolOptions) {
    JSONObject script = JsonUtil.getAsJSON(JsonUtil.getBody(response), V8Protocol.FRAME_SCRIPT);
    if (ChromeDevToolSessionManager.JAVASCRIPT_VOID.equals(JsonUtil.getAsString(script, "sourceStart")) ||
        script.get(V8Protocol.CONTEXT) != null ||
        V8ProtocolUtil.getScriptType(JsonUtil.getAsLong(script, V8Protocol.BODY_SCRIPT_TYPE)) ==
            Script.Type.NATIVE) {
      return null;
    }
    return V8ProtocolUtil.validScript(
        script, JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS), protocolOptions);
  }

}
