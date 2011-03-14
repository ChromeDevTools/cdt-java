// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.output;

import org.chromium.sdk.internal.wip.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class EvaluateOnCallFrame extends JSONObject {
  public EvaluateOnCallFrame(String expression, int callFrameOrdinal,
      int callFrameInjectedScriptId, String objectGroup, boolean includeCommandLineAPI) {
    this.put(BasicConstants.Property.DOMAIN, BasicConstants.Domain.DEBUGGER);
    this.put(BasicConstants.Property.COMMAND, "evaluateOnCallFrame");

    {
      JSONObject arguments = new JSONObject();

      {
        JSONObject callFrameId = new JSONObject();
        callFrameId.put("ordinal", callFrameOrdinal);
        callFrameId.put("injectedScriptId", callFrameInjectedScriptId);
        arguments.put("callFrameId", callFrameId);
      }

      arguments.put("expression", expression);
      arguments.put("objectGroup", objectGroup);
      arguments.put("includeCommandLineAPI", includeCommandLineAPI);
      this.put(BasicConstants.Property.ARGUMENTS, arguments);
    }
  }

}
