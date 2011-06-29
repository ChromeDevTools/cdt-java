// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "clearBreakpoint" V8 request message.
 */
public class ClearBreakpointMessage extends ContextlessDebuggerMessage {

  /**
   * @param breakpoint id in V8 to clear
   */
  public ClearBreakpointMessage(Long breakpoint) {
    super(DebuggerCommand.CLEARBREAKPOINT.value);
    putArgument("breakpoint", breakpoint);
  }
}
