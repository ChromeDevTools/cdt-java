// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.internal.protocol.CommandResponse;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;

/**
 * The callback that handles JSON response to a VM command. The command-sender is staying
 * blocked until callback finishes, which allows the callback to return a result of
 * user-specified type {@code RES}.
 * <p>User should subclass this and implement
 * {@link #handleSuccessfulResponse(SuccessCommandResponse)} method.
 * @param <RES> type of result value that is passed back to caller
 */
public abstract class V8BlockingCallback<RES> {
  public RES messageReceived(CommandResponse response) {
    SuccessCommandResponse successResponse = response.asSuccess();
    if (successResponse == null) {
      throw new RuntimeException("Unsuccessful command " +
          response.asFailure().message());
    }
    return handleSuccessfulResponse(successResponse);
  }

  /**
   * User-implementable method that handled successful json response and pass result back to
   * command-sender.
   * @param response with "success=true"
   */
  protected abstract RES handleSuccessfulResponse(SuccessCommandResponse response);
}
