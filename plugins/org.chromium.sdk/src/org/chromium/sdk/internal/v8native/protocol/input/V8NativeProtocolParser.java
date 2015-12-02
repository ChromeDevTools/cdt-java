// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.protocol.input.data.ContextData;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.json.simple.JSONObject;

/**
 * Interface to native V8 debug protocol parser.
 * @see http://code.google.com/p/v8/wiki/DebuggerProtocol
 */
@JsonParserRoot
public interface V8NativeProtocolParser extends V8NativeProtocolParserTestAccess {

  @JsonParseMethod
  IncomingMessage parseIncomingMessage(JSONObject json) throws JsonProtocolParseException;

  @JsonParseMethod
  SuccessCommandResponse parseSuccessCommandResponse(JSONObject json)
      throws JsonProtocolParseException;

  @JsonParseMethod
  ContextData parseContextData(JSONObject dataObject) throws JsonProtocolParseException;

  @JsonParseMethod
  ValueHandle parseValueHandle(JSONObject value) throws JsonProtocolParseException;
}
