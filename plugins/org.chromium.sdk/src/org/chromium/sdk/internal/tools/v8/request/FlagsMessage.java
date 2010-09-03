// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import java.util.Map;

import org.chromium.sdk.internal.tools.v8.DebuggerCommand;
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
