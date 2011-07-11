// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.ContextBuilder;
import org.chromium.sdk.internal.v8native.DebugSession;
import org.chromium.sdk.internal.v8native.DebuggerCommand;
import org.chromium.sdk.internal.v8native.V8CommandProcessor;
import org.chromium.sdk.internal.v8native.protocol.input.BacktraceCommandBody;
import org.chromium.sdk.internal.v8native.protocol.input.CommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.FrameObject;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;
import org.chromium.sdk.internal.v8native.value.ValueLoader;
import org.chromium.sdk.internal.v8native.value.ValueLoaderImpl;

/**
 * Handles the "backtrace" V8 command replies.
 */
class BacktraceProcessor implements V8CommandProcessor.V8HandlerCallback {

  private final ContextBuilder.ExpectingBacktraceStep step2;

  BacktraceProcessor(ContextBuilder.ExpectingBacktraceStep step2) {
    this.step2 = step2;
 }

  public void messageReceived(CommandResponse response) {
    String commandString = response.command();

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
      body = response.body().asBacktraceCommandBody();
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }
    List<FrameObject> jsonFrames = body.frames();
    if (jsonFrames == null) {
      jsonFrames = Collections.emptyList();
    }

    ValueLoaderImpl valueLoader = step2.getInternalContext().getValueLoader();
    for (SomeHandle handle : response.refs()) {
      valueLoader.addHandleFromRefs(handle);
    }

    return step2.setFrames(jsonFrames);
  }

  public void failure(String message) {
    handleWrongStacktrace();
  }

  private void handleWrongStacktrace() {
    step2.getInternalContext().getContextBuilder().buildSequenceFailure();
  }
}
