// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol.output;

import org.chromium.sdk.internal.rynda.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class RyndaRequest extends JSONObject {
  public RyndaRequest(String domain, String command, RyndaArguments arguments) {
    this.put(BasicConstants.Property.DOMAIN, domain);
    this.put(BasicConstants.Property.COMMAND, command);
    this.put(BasicConstants.Property.ARGUMENTS, arguments);
  }
}
