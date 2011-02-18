// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
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
    private final Map<Breakpoint, WrappedBreakpoint> sdkToUiMap =
        new HashMap<Breakpoint, WrappedBreakpoint>();
    private final Map<WrappedBreakpoint, Breakpoint> uiToSdkMap =
        new HashMap<WrappedBreakpoint, Breakpoint>();

    public InTargetMap() {
    }

    public synchronized Breakpoint getSdkBreakpoint(WrappedBreakpoint chromiumLineBreakpoint) {
      return ChromiumDebugPluginUtil.getSafe(uiToSdkMap, chromiumLineBreakpoint);
    }

    public synchronized WrappedBreakpoint getUiBreakpoint(Breakpoint sdkBreakpoint) {
      return ChromiumDebugPluginUtil.getSafe(sdkToUiMap, sdkBreakpoint);
    }

    public synchronized void add(Breakpoint sdkBreakpoint, WrappedBreakpoint uiBreakpoint) {
      Object conflict1 = uiToSdkMap.put(uiBreakpoint, sdkBreakpoint);
      Object conflict2 = sdkToUiMap.put(sdkBreakpoint, uiBreakpoint);
      if (conflict1 != null || conflict2 != null) {
        throw new RuntimeException();
      }
    }

    public synchronized void remove(WrappedBreakpoint lineBreakpoint) {
      Breakpoint sdkBreakpoint = ChromiumDebugPluginUtil.removeSafe(uiToSdkMap, lineBreakpoint);
      if (sdkBreakpoint == null) {
        throw new RuntimeException();
      }
      ChromiumDebugPluginUtil.removeSafe(sdkToUiMap, sdkBreakpoint);
    }

    public synchronized void clear() {
      sdkToUiMap.clear();
      uiToSdkMap.clear();
    }
  }
}
