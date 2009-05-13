// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "clearBreakpoint" V8 request message.
 */
public class ClearBreakpointRequestMessage extends V8DebugRequestMessage {

  /**
   * 
   * @param breakpoint
   *          number of the breakpoint to clear
   */
  public ClearBreakpointRequestMessage(Integer breakpoint) {
    super(V8Command.CLEARBREAKPOINT.value);
    putArgument("breakpoint", breakpoint); //$NON-NLS-1$
  }
}
