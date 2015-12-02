// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

/**
 * An extension to supported breakpoint target types. The extension is put separate because
 * {@link JavascriptVm} may or may not support it (depends on back-end and remote VM).
 * Each additional breakpoint target type has a dedicated XXXSupport interface and the
 * corresponding getter. Getter returns null if the type is unsupported by the current
 * {@link JavascriptVm}. The support interface provides target constructor method and
 * additional visitor subinterface that {@link Breakpoint.Target#accept} will recognize.
 * <p>
 * The instance may be obtained by {@link JavascriptVm#getBreakpointTypeExtension()}.
 * <p>
 * This API is designed to keep user code fully compilable when new extension is added.
 * This API doesn't allow to add user target types.
 */
public interface BreakpointTypeExtension {

  /**
   * Supports targets that refer to function text in form of function-returning
   * JavaScript expression.
   * E.g. you can set a breakpoint on the 5th line of user method addressed as
   * 'PropertiesDialog.prototype.loadData'.
   * Expression is calculated immediately and never recalculated again.
   */
  interface FunctionSupport {
    /**
     * @return not null
     */
    Breakpoint.Target createTarget(String expression);

    /**
     * Additional interface that user visitor may implement for {@link Breakpoint.Target#accept}
     * method.
     */
    interface Visitor<R> extends Breakpoint.Target.Visitor<R> {
      R visitFunction(String expression);
    }
  }

  /**
   * @return null if 'function' target type is unsupported.
   */
  FunctionSupport getFunctionSupport();

  /**
   * Supports targets that refer to a script by a 'regexp' of its name.
   * After {@link JavascriptVm#setBreakpoint} is
   * called, breakpoint will be set on every script currently loaded in VM whose name matches.
   * E.g. you can safely set a breakpoint before the script is actually loaded.
   */
  interface ScriptRegExpSupport {
    /**
     * @param regExp JavaScript RegExp
     * @return not null
     */
    Breakpoint.Target createTarget(String regExp);

    /**
     * Additional interface that user visitor may implement for {@link Breakpoint.Target#accept}
     * method.
     */
    interface Visitor<R> extends Breakpoint.Target.Visitor<R> {
      /**
       * @param regExp regular expression pattern (as specified in JavaScript) that will be
       *     used to match script names
       */
      R visitRegExp(String regExp);
    }
  }

  /**
   * @return null if 'regexp' target type is unsupported.
   */
  ScriptRegExpSupport getScriptRegExpSupport();
}
