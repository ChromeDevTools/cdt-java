// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import java.util.List;

import org.chromium.sdk.internal.rynda.protocol.input.RyndaCommandResponse;
import org.chromium.sdk.internal.tools.v8.BaseCommandProcessor;

/**
 * An explicit interface for a generic type {@link BaseCommandProcessor.Callback}.
 */
public interface RyndaCommandCallback extends BaseCommandProcessor.Callback<RyndaCommandResponse> {

  /**
   * A default implementation of the callback that separates error responses from
   * success responses.
   */
  abstract class Default implements RyndaCommandCallback {
    protected abstract void onSuccess(RyndaCommandResponse.Success success);
    protected abstract void onError(String message);

    @Override
    public void messageReceived(RyndaCommandResponse response) {
      RyndaCommandResponse.Success asSuccess = response.asSuccess();
      if (asSuccess != null) {
        onSuccess(asSuccess);
      } else if (response.asStub() != null) {
        RyndaBrowserImpl.throwUnsupported();
      } else {
        String message;
        RyndaCommandResponse.Error asError = response.asError();
        if (asError == null) {
          message = "Internal messaging error";
        } else {
          List<String> messages = asError.errors();
          if (messages.size() == 1) {
            message = messages.get(0);
          } else {
            message = messages.toString();
          }
        }
        onError(message);
      }
    }

    @Override
    public void failure(String message) {
      onError(message);
    }
  }
}
