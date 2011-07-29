// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.internal.v8native.protocol.input.CommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;

/**
 * A basic implementation of {@link V8CommandProcessor.V8HandlerCallback} that introduces
 * command success and failure handlers and dispatches the V8 response accordingly.
 */
public abstract class V8CommandCallbackBase implements V8CommandProcessor.V8HandlerCallback {
  public abstract void success(SuccessCommandResponse successResponse);

  public abstract void failure(String message);

  public void messageReceived(CommandResponse response) {
    SuccessCommandResponse successResponse = response.asSuccess();
    if (successResponse == null) {
      this.failure("Remote error: " + response.asFailure().message());
      return;
    } else {
      success(successResponse);
    }
  }
}
