// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import org.chromium.sdk.internal.ContextToken;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "frame" V8 request message.
 */
public class FrameMessage extends DebuggerMessage {

  /**
   * @param frame number (top is 0)
   * @param token the context validity token
   */
  public FrameMessage(Integer frame, ContextToken token) {
    super(DebuggerCommand.FRAME.value, token);
    putArgument("number", frame);
  }
}
