// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
