// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.output;

import org.chromium.sdk.internal.wip.ScriptUrlOrId;
import org.chromium.sdk.internal.wip.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class SetJavaScriptBreakpoint extends JSONObject {
  public SetJavaScriptBreakpoint(ScriptUrlOrId target,
      int lineNumber,int columnNumber, String condition, boolean enabled) {
    this.put(BasicConstants.Property.DOMAIN, BasicConstants.Domain.DEBUGGER);
    this.put(BasicConstants.Property.COMMAND, "setJavaScriptBreakpoint");

    final JSONObject arguments = new JSONObject();

    target.accept(new ScriptUrlOrId.Visitor<Void>() {
      @Override public Void forId(long sourceId) {
        arguments.put("sourceID", sourceId);
        return null;
      }
      @Override public Void forUrl(String url) {
        arguments.put("url", url);
        return null;
      }
    });

    arguments.put("lineNumber", lineNumber);
    arguments.put("columnNumber", columnNumber);
    arguments.put("enabled", enabled);
    if (condition == null) {
      condition = "";
    }
    arguments.put("condition", condition);

    this.put(BasicConstants.Property.ARGUMENTS, arguments);
  }
}
