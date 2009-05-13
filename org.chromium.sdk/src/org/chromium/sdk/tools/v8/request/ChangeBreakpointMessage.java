// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "changeBreakpoint" V8 request message.
 */
public class ChangeBreakpointRequestMessage extends V8DebugRequestMessage {

  /**
   * @param breakpoint
   *          id in V8
   * @param enabled
   *          initial enabled state. True or false, default is true
   * @param condition
   *          string with break point condition
   * @param ignoreCount
   *          number specifying the number of break point hits to ignore,
   *          default value is 0
   */
  public ChangeBreakpointRequestMessage(Integer breakpoint, Boolean enabled,
      String condition, Integer ignoreCount) {
    super(V8Command.CHANGEBREAKPOINT.value);
    putArgument("breakpoint", breakpoint); //$NON-NLS-1$
    putArgument("enabled", enabled); //$NON-NLS-1$
    putArgument("condition", condition); //$NON-NLS-1$
    putArgument("ignoreCount", ignoreCount); //$NON-NLS-1$
  }
}
