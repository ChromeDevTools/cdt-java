// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Breakpoint;

/**
 * TODO(peter.rybin): this class is obsolete, it only holds useful inner class;
 * consider removing this class.
 */
public class BreakpointMap {

  /**
   * A one-to-one map between SDK and UI breakpoints inside one debug target.
   */
  public static class InTargetMap {
    private final Map<Breakpoint, ChromiumLineBreakpoint> sdkToUiMap =
        new HashMap<Breakpoint, ChromiumLineBreakpoint>();
    private final Map<ChromiumLineBreakpoint, Breakpoint> uiToSdkMap =
      new HashMap<ChromiumLineBreakpoint, Breakpoint>();

    public InTargetMap() {
    }

    public Breakpoint getSdkBreakpoint(ChromiumLineBreakpoint chromiumLineBreakpoint) {
      return uiToSdkMap.get(chromiumLineBreakpoint);
    }

    public ChromiumLineBreakpoint getUiBreakpoint(Breakpoint sdkBreakpoint) {
      return sdkToUiMap.get(sdkBreakpoint);
    }

    public void add(Breakpoint sdkBreakpoint, ChromiumLineBreakpoint uiBreakpoint) {
      Object conflict1 = uiToSdkMap.put(uiBreakpoint, sdkBreakpoint);
      Object conflict2 = sdkToUiMap.put(sdkBreakpoint, uiBreakpoint);
      if (conflict1 != null || conflict2 != null) {
        throw new RuntimeException();
      }
    }

    public void remove(ChromiumLineBreakpoint lineBreakpoint) {
      Breakpoint sdkBreakpoint = uiToSdkMap.remove(lineBreakpoint);
      if (sdkBreakpoint == null) {
        throw new RuntimeException();
      }
      sdkToUiMap.remove(sdkBreakpoint);
    }
  }
}
