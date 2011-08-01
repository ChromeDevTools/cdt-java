// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Set;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;

/**
 * Implements breakpoint adapter for breakpoints provided by org.chromium.debug.*
 */
public class ChromiumBreakpointAdapter implements JavaScriptBreakpointAdapter {
  public WrappedBreakpoint tryWrapBreakpoint(IBreakpoint breakpoint) {
    if (!supportsBreakpoint(breakpoint)) {
      return null;
    }
    if (breakpoint instanceof ChromiumLineBreakpoint == false) {
      return null;
    }
    ChromiumLineBreakpoint chromiumLineBreakpoint = (ChromiumLineBreakpoint) breakpoint;
    return wrap(chromiumLineBreakpoint);
  }

  public String getModelId() {
    return VProjectWorkspaceBridge.DEBUG_MODEL_ID;
  }

  private boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return VProjectWorkspaceBridge.DEBUG_MODEL_ID.equals(breakpoint.getModelIdentifier());
  }

  public static WrappedBreakpoint wrap(ChromiumLineBreakpoint chromiumLineBreakpoint) {
    return new WrapperImpl(chromiumLineBreakpoint);
  }

  private static class WrapperImpl extends WrappedBreakpoint {
    private final ChromiumLineBreakpoint chromiumLineBreakpoint;

    WrapperImpl(ChromiumLineBreakpoint chromiumLineBreakpoint) {
      this.chromiumLineBreakpoint = chromiumLineBreakpoint;
    }

    @Override public ILineBreakpoint getInner() {
      return chromiumLineBreakpoint;
    }

    @Override public Set<MutableProperty> getChangedProperty(IMarkerDelta delta) {
      return chromiumLineBreakpoint.getChangedProperty(delta);
    }

    @Override public String getCondition() throws CoreException {
      return chromiumLineBreakpoint.getCondition();
    }

    @Override public IgnoreCountData getIgnoreCountData() {
      return chromiumLineBreakpoint.getIgnoreCountData();
    }

    @Override public void setIgnoreCountData(IgnoreCountData data) throws CoreException {
      chromiumLineBreakpoint.setIgnoreCountData(data);
    }
  }
}
