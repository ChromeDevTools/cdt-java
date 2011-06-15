// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * An extension to supported breakpoint target types. The extension is put separate because
 * {@link JavascriptVm} may or may not support it (depends on back-end and remote VM).
 * Each additional breakpoint target type has a dedicated XXXSupport interface and the
 * corresponding getter. Getter returns null if the type is unsupported by the current
 * {@link JavascriptVm}. The support interface provides target constructor method and
 * additional visitor subinterface that {@link Breakpoint.Target#accept()} will recognize.
 * <p>
 * The instance may be obtained by {@link JavascriptVm#getBreakpointTypeExtension()}.
 * <p>
 * This API is designed to keep user code fully compilable when new extension is added.
 * This API doesn't allow to add user target types.
 */
public interface BreakpointTypeExtension {

  /**
   * Support for 'function' breakpoint target: breakpoint is being set to a function
   * that is returned by JavaScript expression.
   */
  interface FunctionSupport {
    Breakpoint.Target createTarget(String expression);

    interface Visitor<R> extends Breakpoint.Target.Visitor<R> {
      R visitFunction(String expression);
    }
  }

  /**
   * @return null if 'function' target type is unsupported.
   */
  FunctionSupport getFunctionSupport();
}
