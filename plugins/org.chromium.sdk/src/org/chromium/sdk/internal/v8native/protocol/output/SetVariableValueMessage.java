// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;
import org.json.simple.JSONObject;

/**
 * Represents a "setVariableValue" request message.
 */
public class SetVariableValueMessage extends DebuggerMessage {
  public SetVariableValueMessage(ScopeMessage.Ref scopeRef, String variableName,
      EvaluateMessage.Value value) {
    super(DebuggerCommand.SETVARIABLEVALUE.value);
    JSONObject scopeObject = new JSONObject();
    scopeRef.fillJson(scopeObject);
    putArgument("scope", scopeObject);
    putArgument("name", variableName);
    putArgument("newValue", value.createJsonParameter());
  }
}
