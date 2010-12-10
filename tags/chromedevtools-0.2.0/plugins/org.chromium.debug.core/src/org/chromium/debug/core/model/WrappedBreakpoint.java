// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ILineBreakpoint;

/**
 * An abstract representation of a JavaScript line breakpoint. The actual breakpoints may
 * be provided by different plugins and have incompatible interfaces.
 * <p>
 * It implements equals/hashcode operations to be usable in Java collections.
 */
public abstract class WrappedBreakpoint {
  public abstract ILineBreakpoint getInner();

  public abstract String getCondition() throws CoreException;
  public abstract int getIgnoreCount() throws CoreException;
  public abstract void setIgnoreCount(int i) throws CoreException;

  
  public boolean equals(Object obj) {
    if (obj instanceof WrappedBreakpoint == false) {
      return false;
    }
    WrappedBreakpoint other = (WrappedBreakpoint) obj;
    return this.getInner().equals(other.getInner());
  }
  
  public int hashCode() {
    return getInner().hashCode();
  }
}
