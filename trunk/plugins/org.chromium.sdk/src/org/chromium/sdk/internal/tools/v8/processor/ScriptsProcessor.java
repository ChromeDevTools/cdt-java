// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.ScriptManager;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the "scripts" V8 command replies.
 */
public class ScriptsProcessor extends V8ResponseCallback {

  public ScriptsProcessor(DebugContextImpl context) {
    super(context);
  }

  @Override
  public void messageReceived(JSONObject response) {
    JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
    ScriptManager scriptManager = getDebugContext().getScriptManager();
    for (int i = 0; i < body.size(); ++i) {
      JSONObject scriptJson = (JSONObject) body.get(i);
      scriptManager.addScript(scriptJson);
    }
  }

}
