// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.internal.v8native.protocol.input.CommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;

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
