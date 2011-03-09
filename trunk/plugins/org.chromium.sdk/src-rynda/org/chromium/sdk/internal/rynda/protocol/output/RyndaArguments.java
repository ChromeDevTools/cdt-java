// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol.output;

import org.chromium.sdk.internal.rynda.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class RyndaArguments extends JSONObject {
  public RyndaArguments(long injectedScriptId, String methodName,
      String additionalArguments) {
    this.put("injectedScriptId", injectedScriptId);
    this.put("methodName", methodName);
    this.put(BasicConstants.Property.ARGUMENTS, additionalArguments);
  }

  public RyndaArguments() {
  }
}
