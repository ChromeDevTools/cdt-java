// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "clearBreakpoint" V8 request message.
 */
public class ClearBreakpointMessage extends DebuggerMessage {

  /**
   * @param breakpoint id in V8 to clear
   */
  public ClearBreakpointMessage(Long breakpoint) {
    super(DebuggerCommand.CLEARBREAKPOINT.value);
    putArgument("breakpoint", breakpoint);
  }
}
