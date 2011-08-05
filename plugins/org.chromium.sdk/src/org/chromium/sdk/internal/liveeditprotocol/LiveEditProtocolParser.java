// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.liveeditprotocol;

import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.json.simple.JSONObject;

/**
 * Interface to LiveEdit protocol parser.
 */
@JsonParserRoot
public interface LiveEditProtocolParser {

  @JsonParseMethod
  LiveEditResult parseLiveEditResult(JSONObject underlyingObject) throws JsonProtocolParseException;

}
