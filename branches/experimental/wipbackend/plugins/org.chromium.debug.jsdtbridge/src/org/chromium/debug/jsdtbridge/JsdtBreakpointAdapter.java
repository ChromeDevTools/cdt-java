// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.jsdtbridge;

import org.chromium.debug.core.model.JavaScriptBreakpointAdapter;
import org.chromium.debug.core.model.WrappedBreakpoint;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;

/**
 * Implements breakpoint adapter for breakpoints provided by JSDT.
 */
public class JsdtBreakpointAdapter implements JavaScriptBreakpointAdapter {
  public WrappedBreakpoint tryWrapBreakpoint(IBreakpoint breakpoint) {
    if (breakpoint instanceof IJavaScriptLineBreakpoint == false) {
      return null;
    }
    if (!breakpoint.getModelIdentifier().equals(getModelId())) {
      return null;
    }
    IJavaScriptLineBreakpoint jsdtBreakpoint = (IJavaScriptLineBreakpoint) breakpoint;
    return wrap(jsdtBreakpoint);
  }

  public String getModelId() {
    return JavaScriptDebugModel.MODEL_ID;
  }

  private static WrappedBreakpoint wrap(final IJavaScriptLineBreakpoint breakpoint) {
    return new WrapperImpl(breakpoint);
  }

  private static final class WrapperImpl extends WrappedBreakpoint {
    private final IJavaScriptLineBreakpoint breakpoint;

    WrapperImpl(IJavaScriptLineBreakpoint breakpoint) {
      this.breakpoint = breakpoint;
    }

    @Override
    public ILineBreakpoint getInner() {
      return breakpoint;
    }


    public String getCondition() throws CoreException {
      return breakpoint.getCondition();
    }

    public int getIgnoreCount() throws CoreException {
      return breakpoint.getHitCount();
    }

    public void setIgnoreCount(int count) throws CoreException {
      breakpoint.setHitCount(count);
    }
  }
}
