// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
