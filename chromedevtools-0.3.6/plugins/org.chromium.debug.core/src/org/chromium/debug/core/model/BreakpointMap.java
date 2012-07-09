// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.getSafe;
import static org.chromium.sdk.util.BasicUtil.removeSafe;

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

    public synchronized Breakpoint getSdkBreakpoint(ChromiumLineBreakpoint chromiumLineBreakpoint) {
      return getSafe(uiToSdkMap, chromiumLineBreakpoint);
    }

    public synchronized ChromiumLineBreakpoint getUiBreakpoint(Breakpoint sdkBreakpoint) {
      return getSafe(sdkToUiMap, sdkBreakpoint);
    }

    public synchronized void add(Breakpoint sdkBreakpoint, ChromiumLineBreakpoint uiBreakpoint) {
      Object conflict1 = uiToSdkMap.put(uiBreakpoint, sdkBreakpoint);
      Object conflict2 = sdkToUiMap.put(sdkBreakpoint, uiBreakpoint);
      if (conflict1 != null || conflict2 != null) {
        throw new RuntimeException();
      }
    }

    public synchronized void remove(ChromiumLineBreakpoint lineBreakpoint) {
      Breakpoint sdkBreakpoint = removeSafe(uiToSdkMap, lineBreakpoint);
      if (sdkBreakpoint == null) {
        throw new RuntimeException();
      }
      removeSafe(sdkToUiMap, sdkBreakpoint);
    }

    public synchronized void clear() {
      sdkToUiMap.clear();
      uiToSdkMap.clear();
    }
  }
}
