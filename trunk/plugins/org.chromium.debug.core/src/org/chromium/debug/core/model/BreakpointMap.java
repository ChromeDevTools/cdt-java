// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.Breakpoint;

/**
 * A map between breakpoints in ChromeDevTools SDK and in Eclipse debugger framework.
 */
public class BreakpointMap {

  /**
   * For Eclipse UI breakpoint returns all corresponding breakpoints in SDK. Since several debug
   * launches/targets may be opened for the same set of source files, one UI breakpoint may
   * correspond to several SDK breakpoints in different debug launches/targets.
   */
  public Collection<BreakpointMapping> getSdkBreakpoints(
      ChromiumLineBreakpoint chromiumLineBreakpoint) {
    List<BreakpointMapping> res = new ArrayList<BreakpointMapping>(inTargetMaps.size());
    for (InTargetMap map : inTargetMaps) {
      Breakpoint sdkBreakpoint = map.getSdkBreakpoint(chromiumLineBreakpoint);
      if (sdkBreakpoint == null) {
        continue;
      }
      res.add(new BreakpointMapping(sdkBreakpoint, map.getDebugTarget()));
    }
    return res;
  }

  public InTargetMap addTarget(DebugTargetImpl debugTarget) {
    InTargetMap inTargetMap = new InTargetMap(debugTarget);
    inTargetMaps.add(inTargetMap);
    return inTargetMap;
  }

  private final List<InTargetMap> inTargetMaps = new ArrayList<InTargetMap>(3);


  /**
   * A one-to-one map between SDK and UI breakpoints inside one debug target.
   */
  public class InTargetMap {
    private final DebugTargetImpl debugTarget;
    private final Map<Breakpoint, ChromiumLineBreakpoint> sdkToUiMap =
        new HashMap<Breakpoint, ChromiumLineBreakpoint>();
    private final Map<ChromiumLineBreakpoint, Breakpoint> uiToSdkMap =
      new HashMap<ChromiumLineBreakpoint, Breakpoint>();

    private InTargetMap(DebugTargetImpl debugTarget) {
      this.debugTarget = debugTarget;
    }

    private Breakpoint getSdkBreakpoint(ChromiumLineBreakpoint chromiumLineBreakpoint) {
      return uiToSdkMap.get(chromiumLineBreakpoint);
    }

    public ChromiumLineBreakpoint getUiBreakpoint(Breakpoint sdkBreakpoint) {
      return sdkToUiMap.get(sdkBreakpoint);
    }

    public DebugTargetImpl getDebugTarget() {
      return debugTarget;
    }

    public void dispose() {
      boolean res = inTargetMaps.remove(this);
      if (!res) {
        throw new IllegalStateException();
      }
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

  public static class BreakpointMapping {
    private final Breakpoint sdkBreakpoint;
    private final DebugTargetImpl debugTargetImpl;

    BreakpointMapping(Breakpoint sdkBreakpoint, DebugTargetImpl debugTargetImpl) {
      this.sdkBreakpoint = sdkBreakpoint;
      this.debugTargetImpl = debugTargetImpl;
    }

    public Breakpoint getSdkBreakpoint() {
      return sdkBreakpoint;
    }

    public DebugTargetImpl getDebugTarget() {
      return debugTargetImpl;
    }
  }
}
