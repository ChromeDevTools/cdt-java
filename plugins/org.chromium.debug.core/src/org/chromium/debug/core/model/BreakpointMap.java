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
  public static class InTargetMap<SDK, UI> {
    private final Map<SDK, UI> sdkToUiMap = new HashMap<SDK, UI>();
    private final Map<UI, SDK> uiToSdkMap = new HashMap<UI, SDK>();

    public InTargetMap() {
    }

    public synchronized SDK getSdkBreakpoint(UI uiBreakpoint) {
      return getSafe(uiToSdkMap, uiBreakpoint);
    }

    public synchronized UI getUiBreakpoint(SDK sdkBreakpoint) {
      return getSafe(sdkToUiMap, sdkBreakpoint);
    }

    public synchronized void add(SDK sdkBreakpoint, UI uiBreakpoint) {
      Object conflict1 = uiToSdkMap.put(uiBreakpoint, sdkBreakpoint);
      Object conflict2 = sdkToUiMap.put(sdkBreakpoint, uiBreakpoint);
      if (conflict1 != null || conflict2 != null) {
        throw new RuntimeException();
      }
    }

    public synchronized void remove(UI uiBreakpoint) {
      SDK sdkBreakpoint = removeSafe(uiToSdkMap, uiBreakpoint);
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
