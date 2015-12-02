// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
