// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.json.simple.JSONObject;

/**
 * Interface to WIP protocol parser. It includes generated part {@link WipGeneratedParserRoot}.
 */
@JsonParserRoot
public interface WipProtocolParser extends WipGeneratedParserRoot {

  @JsonParseMethod
  WipCommandResponse parseWipCommandResponse(JSONObject incoming)
      throws JsonProtocolParseException;

  @JsonParseMethod
  WipEvent parseWipEvent(JSONObject jsonObject) throws JsonProtocolParseException;

  @JsonParseMethod
  WipTabList parseTabList(Object jsonValue) throws JsonProtocolParseException;

}
