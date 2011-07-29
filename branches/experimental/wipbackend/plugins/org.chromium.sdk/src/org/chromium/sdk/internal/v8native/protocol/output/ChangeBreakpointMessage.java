// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "changeBreakpoint" V8 request message.
 */
public class ChangeBreakpointMessage extends ContextlessDebuggerMessage {

  /**
   * @param breakpoint id in V8
   * @param enabled nullable initial enabled state. Default is true
   * @param condition nullable string with break point condition
   * @param ignoreCount nullable number specifying the number of break point hits to ignore.
   *        Default is 0
   */
  public ChangeBreakpointMessage(Long breakpoint, Boolean enabled,
      String condition, Integer ignoreCount) {
    super(DebuggerCommand.CHANGEBREAKPOINT.value);
    putArgument("breakpoint", breakpoint);
    putArgument("enabled", enabled);
    putNullableArgument("condition", condition);
    putArgument("ignoreCount", ignoreCount);
  }
}
