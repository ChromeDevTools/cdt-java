// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol.output;

import org.chromium.sdk.internal.rynda.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class GetScriptSource extends JSONObject {
  public GetScriptSource(long sourceID) {
    this.put(BasicConstants.Property.DOMAIN, BasicConstants.Domain.DEBUGGER);
    this.put(BasicConstants.Property.COMMAND, "getScriptSource");
    JSONObject arguments;
    {
      arguments = new JSONObject();
      arguments.put("sourceID", "" + sourceID);
    }
    this.put(BasicConstants.Property.ARGUMENTS, arguments);
  }
}
