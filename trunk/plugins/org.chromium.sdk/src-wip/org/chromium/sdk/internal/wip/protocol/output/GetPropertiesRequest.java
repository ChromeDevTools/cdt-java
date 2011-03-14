// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.output;

import org.chromium.sdk.internal.wip.protocol.BasicConstants;
import org.json.simple.JSONObject;

public class GetPropertiesRequest extends JSONObject {
  public GetPropertiesRequest(long id, long injectedScriptId,
      boolean ignoreHasOwnProperty, boolean abbreviate) {
    this.put(BasicConstants.Property.DOMAIN, BasicConstants.Domain.RUNTIME);
    this.put(BasicConstants.Property.COMMAND, "getProperties");
    {
      JSONObject arguments = new JSONObject();
      {
        JSONObject objectIdObject = new JSONObject();
        objectIdObject.put("id", id);
        objectIdObject.put("injectedScriptId", injectedScriptId);
        arguments.put("objectId", objectIdObject);
      }
      arguments.put("ignoreHasOwnProperty", ignoreHasOwnProperty);
      arguments.put("abbreviate", abbreviate);

      this.put(BasicConstants.Property.ARGUMENTS, arguments);
    }
  }
}
