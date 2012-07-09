// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "restartframe" experimental V8 request message.
 */
public class RestartFrameMessage extends DebuggerMessage {
  /**
   * @param frame number (top is 0).
   */
  public RestartFrameMessage(Integer frame) {
    super(DebuggerCommand.RESTARTFRAME.value);
    if (frame != null) {
      putArgument("frame", frame);
    }
  }
}
