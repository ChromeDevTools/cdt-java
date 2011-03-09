// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol.output;

import org.chromium.sdk.internal.rynda.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class RemoveBreakpointRequest extends JSONObject {
  public RemoveBreakpointRequest(String id) {
    this.put(BasicConstants.Property.DOMAIN, BasicConstants.Domain.DEBUGGER);
    this.put(BasicConstants.Property.COMMAND, "removeJavaScriptBreakpoint");
    {
      JSONObject arguments = new JSONObject();
      arguments.put("breakpointId", id);
      this.put(BasicConstants.Property.ARGUMENTS, arguments);
    }
  }
}
