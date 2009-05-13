// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "backtrace" V8 request message.
 */
public class BacktraceRequestMessage extends V8DebugRequestMessage {

  /**
   * @param fromFrame
   *          nullable frame range start
   * @param toFrame
   *          nullable frame range end
   */
  public BacktraceRequestMessage(Integer fromFrame, Integer toFrame) {
    super(V8Command.BACKTRACE.value);
    putArgument("fromFrame", fromFrame); //$NON-NLS-1$
    putArgument("toFrame", toFrame); //$NON-NLS-1$
  }
}
