// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.getSafe;
import static org.chromium.sdk.util.BasicUtil.removeSafe;

import java.util.HashMap;
import java.util.Map;

/**
 * A one-to-one map between SDK and UI breakpoints inside one debug target.
 */
public class BreakpointInTargetMap<SDK, UI> {
  private final Map<SDK, UI> sdkToUiMap = new HashMap<SDK, UI>();
  private final Map<UI, SDK> uiToSdkMap = new HashMap<UI, SDK>();

  public BreakpointInTargetMap() {
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