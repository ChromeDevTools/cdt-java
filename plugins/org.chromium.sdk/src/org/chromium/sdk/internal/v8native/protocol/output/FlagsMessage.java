// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.output;

import java.util.Map;

import org.chromium.sdk.internal.v8native.DebuggerCommand;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Represents a "flags" V8 request message.
 */
public class FlagsMessage extends ContextlessDebuggerMessage {
  public FlagsMessage(Map<String, Object> flags) {
    super(DebuggerCommand.FLAGS.value);

    if (flags != null) {
      JSONArray flagArray = new JSONArray();
      for (Map.Entry<String, Object> en : flags.entrySet()) {
        JSONObject flagObject = new JSONObject();
        flagObject.put("name", en.getKey());
        if (en.getValue() != null) {
          flagObject.put("value", en.getValue());
        }
        flagArray.add(flagObject);
      }
      putArgument("flags", flagArray);
    }
  }
}
