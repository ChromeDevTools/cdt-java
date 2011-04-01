// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.Collections;
import java.util.List;

import org.chromium.sdk.Script;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.V8ContextFilter;
import org.chromium.sdk.internal.protocol.AfterCompileBody;
import org.chromium.sdk.internal.protocol.EventNotification;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.protocol.data.ScriptHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.ChromeDevToolSessionManager;
import org.chromium.sdk.internal.tools.v8.V8CommandCallbackBase;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;

/**
 * Listens for scripts sent in the "afterCompile" events and requests their
 * sources.
 */
public class AfterCompileProcessor extends V8EventProcessor {

  public AfterCompileProcessor(DebugSession debugSession) {
    super(debugSession);
  }

  @Override
  public void messageReceived(EventNotification eventMessage) {
    final DebugSession debugSession = getDebugSession();
    ScriptHandle script = getScriptToLoad(eventMessage,
        debugSession.getScriptManager().getContextFilter());
    if (script == null) {
      return;
    }
    debugSession.sendMessageAsync(
        DebuggerMessageFactory.scripts(
            Collections.singletonList(V8ProtocolUtil.getScriptIdFromResponse(script)), true),
        true,
        new V8CommandCallbackBase() {
          @Override
          public void success(SuccessCommandResponse successResponse) {
            List<ScriptHandle> body;
            try {
              body = successResponse.body().asScripts();
            } catch (JsonProtocolParseException e) {
              throw new RuntimeException(e);
            }
            // body is an array of scripts
            if (body.size() == 0) {
              return; // The script did not arrive (bad id?)
            }
            Script newScript = debugSession.getScriptManager().addScript(
                body.get(0),
                successResponse.refs());
            if (newScript != null) {
              getDebugSession().getSessionManager().getDebugEventListener().scriptLoaded(newScript);
            }
          }

          @Override
          public void failure(String message) {
            // The script is now missing.
          }
        },
        null);
  }

  private static ScriptHandle getScriptToLoad(EventNotification eventResponse,
      V8ContextFilter contextFilter) {
    AfterCompileBody body;
    try {
      body = eventResponse.body().asAfterCompileBody();
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }
    ScriptHandle script = body.script();
    if (ChromeDevToolSessionManager.JAVASCRIPT_VOID.equals(script.sourceStart()) ||
        script.context() == null ||
        V8ProtocolUtil.getScriptType(script.scriptType()) ==
            Script.Type.NATIVE) {
      return null;
    }
    return V8ProtocolUtil.validScript(script, eventResponse.refs(), contextFilter);
  }
}
