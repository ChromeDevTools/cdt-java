// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.protocol.input.data.ScriptHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeRef;
import org.json.simple.JSONObject;

/**
 * A part of {@link V8NativeProtocolParser} interface that is introduced specially for
 * test disposals.
 */
@JsonParserRoot
public interface V8NativeProtocolParserTestAccess {

  @JsonParseMethod
  FrameObject parseFrameObject(JSONObject jsonObject) throws JsonProtocolParseException;

  @JsonParseMethod
  SomeRef parseSomeRef(JSONObject valueObject) throws JsonProtocolParseException;

  @JsonParseMethod
  ScriptHandle parseScriptHandle(JSONObject body) throws JsonProtocolParseException;
}