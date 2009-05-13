// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.processor;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.chromium.debug.core.tools.v8.V8ReplyHandler;
import org.chromium.debug.core.tools.v8.model.mirror.Execution;
import org.chromium.debug.core.tools.v8.model.mirror.Script;
import org.chromium.debug.core.util.JsonUtil;
import org.chromium.debug.core.util.WorkspaceUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the "scripts" V8 command replies.
 */
public class ScriptsProcessor extends V8ReplyHandler {

  private final Execution execution;

  public ScriptsProcessor(V8DebuggerToolHandler debuggerToolHandler) {
    super(debuggerToolHandler);
    this.execution = debuggerToolHandler.getExecution();
  }

  @Override
  public void handleReply(JSONObject reply) {
    JSONArray body =
        JsonUtil.getAsJSONArray(reply, Protocol.BODY_SCRIPTS);
    IProject debugProject = getToolHandler().getDebugTarget().getDebugProject();
    for (int i = 0; i < body.size(); ++i) {
      JSONObject scriptJson = (JSONObject) body.get(i);
      Script script = execution.getScriptManager().addScript(scriptJson);
      if (script != null && script.getResourceName() == null) {
        String resourceName =
            WorkspaceUtil.createFile(debugProject, script.getName());
        script.setResourceName(resourceName);
        if (script.hasSource()) {
          try {
            WorkspaceUtil.writeFile(
                debugProject, resourceName, script.getSource());
          } catch (CoreException e) {
            ChromiumDebugPlugin.log(e);
          }
        }
      }
    }
  }
}
