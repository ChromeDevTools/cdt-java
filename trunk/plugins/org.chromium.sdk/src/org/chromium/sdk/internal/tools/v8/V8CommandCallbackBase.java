// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.internal.protocol.CommandResponse;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;

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
      this.failure(response.asFailure().getMessage());
      return;
    } else {
      success(successResponse);
    }
  }
}
