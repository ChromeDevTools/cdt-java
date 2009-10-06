// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.internal.JsonUtil;
import org.json.simple.JSONObject;

/**
 * The callback that handles JSON response to a VM command. The command-sender is staying
 * blocked until callback finishes, which allows the callback to return a result of
 * user-specified type {@code RES}.
 * <p>User should subclass this and implement {@link #handleSuccessfulResponse(JSONObject)}
 * method.
 * @param <RES> type of result value that is passed back to caller
 */
public abstract class V8BlockingCallback<RES> {
  public RES messageReceived(JSONObject response) {
    if (!JsonUtil.isSuccessful(response)) {
      throw new RuntimeException("Unsuccessful command");
    }
    return handleSuccessfulResponse(response);
  }

  /**
   * User-implemetable method that handled successful json repsone and pass result back to
   * command-sender.
   * @param response with "success=true"
   */
  protected abstract RES handleSuccessfulResponse(JSONObject response);
}
