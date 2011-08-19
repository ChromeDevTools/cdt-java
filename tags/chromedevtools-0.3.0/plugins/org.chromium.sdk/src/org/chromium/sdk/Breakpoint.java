// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * A breakpoint in the browser JavaScript virtual machine. The {@code set*}
 * method invocations will not take effect until
 * {@link #flush} is called.
 */
public interface Breakpoint {

  /**
   * This value is used when the corresponding parameter is absent.
   *
   * @see JavascriptVm#setBreakpoint
   */
  int EMPTY_VALUE = -1;

  /**
   * A breakpoint has this ID if it does not reflect an actual breakpoint in a
   * JavaScript VM debugger.
   */
  long INVALID_ID = -1;

  /**
   * @return where this breakpoint was targeted to (e.g. script with a particular name or
   *     script with specified id)
   */
  Target getTarget();

  /**
   * @return the breakpoint unique ID or {@link #INVALID_ID} if breakpoint was deleted
   */
  long getId();

  /**
   * Returns line number of the breakpoint. As source is changed (typically with LiveEdit feature,
   * and particularly by calling {@link UpdatableScript#setSourceOnRemote}) this value
   * may become stale. It gets updated when {@link JavascriptVm#listBreakpoints} asynchronous
   * method completes.
   *
   * @return 1-based line number in script source
   */
  long getLineNumber();

  /**
   * @return whether this breakpoint is enabled
   */
  boolean isEnabled();

  /**
   * Sets whether this breakpoint is enabled.
   * Requires subsequent {@link #flush} call.
   * @param enabled whether the breakpoint should be enabled
   */
  void setEnabled(boolean enabled);

  /**
   * @return breakpoint condition as plain JavaScript or {@code null} if none
   */
  String getCondition();

  /**
   * Sets the breakpoint condition as plain JavaScript ({@code null} to clear).
   * Requires subsequent {@link #flush} call.
   * @param condition the new breakpoint condition
   */
  void setCondition(String condition);

  /**
   * Removes the breakpoint from the JS debugger and invokes the
   * callback once the operation has finished. This operation does <strong>not</strong> require
   * a {@code flush} invocation.
   *
   * @param callback to invoke once the operation result is available
   */
  RelayOk clear(JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback);

  /**
   * Flushes the breakpoint parameter changes (set* methods) into the browser
   * and invokes the callback once the operation has finished. This method must
   * be called for the set* method invocations to take effect.
   *
   * @param callback to invoke once the operation result is available
   */
  RelayOk flush(JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback);

  /**
   * @return extension that supports ignore count property of breakpoint, same instance
   *     that {@link JavascriptVm#getBreakpointTypeExtension()} returns
   */
  IgnoreCountBreakpointExtension getIgnoreCountBreakpointExtension();

  /**
   * A reference to some JavaScript text that you can set breakpoints on. The reference may
   * be in form of script name, script id etc.
   * This type is essentially an Algebraic Type with several cases. Additional cases are provided
   * in form of optional extensions.
   * @see Target.ScriptName
   * @see Target.ScriptId
   * @see BreakpointTypeExtension
   */
  abstract class Target {
    /**
     * Dispatches call on the actual Target type.
     * @param visitor user-provided {@link Visitor} that may also implement some additional
     *     interfaces (for extended types) that is checked on runtime
     * @see BreakpointTypeExtension
     */
    public abstract <R> R accept(Visitor<R> visitor);

    public interface Visitor<R> {
      R visitScriptName(String scriptName);
      R visitScriptId(Object scriptId);
      R visitUnknown(Target target);
    }

    /**
     * A target that refers to a script by its id.
     */
    public static class ScriptId extends Target {
      private final Object id;
      public ScriptId(Object id) {
        this.id = id;
      }
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitScriptId(id);
      }
    }

    /**
     * A target that refers to a script by its name. After {@link JavascriptVm#setBreakpoint}
     * is called, breakpoint will be set on every matching script currently loaded in VM.
     * E.g. you can safely set a breakpoint before the script is actually loaded.
     */
    public static class ScriptName extends Target {
      private final String name;
      public ScriptName(String name) {
        this.name = name;
      }
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitScriptName(name);
      }
    }
  }
}
