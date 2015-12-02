// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip.protocol.output;

import org.chromium.sdk.internal.wip.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class WipRequest extends JSONObject {
  public WipRequest(WipParams params) {
    this.put(BasicConstants.Property.METHOD, params.getRequestName());
    this.put(BasicConstants.Property.PARAMS, params);
  }
}
