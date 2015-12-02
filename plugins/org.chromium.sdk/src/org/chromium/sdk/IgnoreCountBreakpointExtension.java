// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.util.GenericCallback;

/**
 * An extension to breakpoint API that supports 'ignore count' property. Some backends
 * or VM versions may not support it.
 * @see JavascriptVm#getIgnoreCountBreakpointExtension()
 * @see Breakpoint#getIgnoreCountBreakpointExtension()
 */
public interface IgnoreCountBreakpointExtension {

  /**
   * This value is used when the corresponding parameter is absent.
   *
   * @see #setIgnoreCount
   */
  int EMPTY_VALUE = Breakpoint.EMPTY_VALUE;

  /**
   * Sets a breakpoint with the specified parameters.
   * @param target of the breakpoint
   * @param line in the script or function (1-based). If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param column of the target start within the line (1-based). If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param enabled whether the breakpoint is enabled initially
   * @param ignoreCount number specifying the amount of breakpoint hits to
   *        ignore. If none, use {@link #EMPTY_VALUE}
   * @param condition nullable string with breakpoint condition
   * @param callback to invoke when the evaluation result is ready,
   *        may be {@code null}
   */
  RelayOk setBreakpoint(JavascriptVm javascriptVm, Breakpoint.Target target, int line, int column,
      boolean enabled, String condition, int ignoreCount,
      BreakpointCallback callback, SyncCallback syncCallback);

  /**
   * Sets the ignore count for this breakpoint ({@code EMPTY_VALUE} to clear).
   * Does <strong>not</strong> require subsequent flush call.
   * @param ignoreCount the new ignored hits count to set
   */
  RelayOk setIgnoreCount(Breakpoint breakpoint, int ignoreCount,
      GenericCallback<Void> callback, SyncCallback syncCallback);
}
