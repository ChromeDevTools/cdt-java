// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import java.util.List;
import java.util.Map;

import org.chromium.sdk.internal.v8native.DebuggerCommand;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Represents an "evaluate" V8 request message.
 */
public class EvaluateMessage extends DebuggerMessage {

  /**
   * @param expression to evaluate
   * @param frame number (top is 0).
   * @param global nullable. Default is false
   * @param disableBreak nullable. Default is true
   * @param additionalContext nullable
   */
  public EvaluateMessage(String expression, Integer frame,
      Boolean global, Boolean disableBreak, List<Map.Entry<String, Integer>> additionalContext) {
    super(DebuggerCommand.EVALUATE.value);
    putArgument("expression", expression);
    if (frame != null) {
      putArgument("frame", frame);
    }
    putArgument("global", global);
    putArgument("disable_break", disableBreak);
    putArgument("inlineRefs", Boolean.TRUE);
    if (additionalContext != null) {
      JSONArray contextParam = new JSONArray();
      for (Map.Entry<String, Integer> en : additionalContext) {
        JSONObject mapping = new JSONObject();
        mapping.put("name", en.getKey());
        mapping.put("handle", en.getValue());
        contextParam.add(mapping);
      }
      putArgument("additional_context", contextParam);
    }
  }
}
