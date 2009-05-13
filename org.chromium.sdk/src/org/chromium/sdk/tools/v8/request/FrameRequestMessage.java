// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "frame" V8 request message.
 */
public class FrameRequestMessage extends V8DebugRequestMessage {

  /**
   * @param number
   *          frame number (top is 0)
   */
  public FrameRequestMessage(Integer number) {
    super(V8Command.FRAME.value);
    putArgument("number", number); //$NON-NLS-1$
  }
}
