// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
