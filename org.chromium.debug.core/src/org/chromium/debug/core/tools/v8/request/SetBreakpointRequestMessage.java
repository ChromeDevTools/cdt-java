// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.BreakpointType;
import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "setbreakpoint" V8 request message.
 */
public class SetBreakpointRequestMessage extends V8DebugRequestMessage {

  /**
   * @param type
   *          "function" or "script"
   * @param target
   *          function expression or script identification
   * @param line
   *          line in script or function
   * @param position
   *          character position within the line
   * @param enabled
   *          initial enabled state. Default is true
   * @param condition
   *          string with breakpoint condition
   * @param ignoreCount
   *          number specifying the number of break point hits to ignore,
   *          default value is 0
   */
  public SetBreakpointRequestMessage(BreakpointType type, String target,
      Integer line, Integer position, Boolean enabled, String condition,
      Integer ignoreCount) {
    super(V8Command.SETBREAKPOINT.value);
    putArgument("type", type.value); //$NON-NLS-1$
    putArgument("target", target); //$NON-NLS-1$
    putArgument("line", line); //$NON-NLS-1$
    putArgument("position", position); //$NON-NLS-1$
    putArgument("enabled", enabled); //$NON-NLS-1$
    putArgument("condition", condition); //$NON-NLS-1$
    putArgument("ignoreCount", ignoreCount); //$NON-NLS-1$
  }
}
