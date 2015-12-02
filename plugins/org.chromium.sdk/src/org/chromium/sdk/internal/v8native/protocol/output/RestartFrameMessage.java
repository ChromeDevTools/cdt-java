// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
