// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;


import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Implements breakpoint adapter for breakpoints provided by org.chromium.debug.*
 */
public class ChromiumBreakpointAdapter {
  public static ChromiumLineBreakpoint tryCastBreakpoint(IBreakpoint breakpoint) {
    if (!supportsBreakpoint(breakpoint)) {
      return null;
    }
    if (breakpoint instanceof ChromiumLineBreakpoint == false) {
      return null;
    }
    return (ChromiumLineBreakpoint) breakpoint;
  }

  private static boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return VProjectWorkspaceBridge.DEBUG_MODEL_ID.equals(breakpoint.getModelIdentifier());
  }
}
