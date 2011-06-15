// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * A breakpoint in the browser JavaScript virtual machine. The {@code set*}
 * method invocations will not take effect until
 * {@link #flush(org.chromium.sdk.JavascriptVm.BreakpointCallback)} is called.
 */
public interface Breakpoint {

  /**
   * This value is used when the corresponding parameter is absent.
   *
   * @see #getIgnoreCount()
   * @see #setIgnoreCount(int)
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
   * @return the breakpoint ID as reported by the JavaScript VM debugger
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
   *
   * @param enabled whether the breakpoint should be enabled
   */
  void setEnabled(boolean enabled);

  /**
   * @return ignore count for this breakpoint or {@code EMPTY_VALUE} if none
   */
  int getIgnoreCount();

  /**
   * Sets the ignore count for this breakpoint ({@code EMPTY_VALUE} to clear).
   *
   * @param ignoreCount the new ignored hits count to set
   */
  void setIgnoreCount(int ignoreCount);

  /**
   * @return breakpoint condition as plain JavaScript or {@code null} if none
   */
  String getCondition();

  /**
   * Sets the breakpoint condition as plain JavaScript ({@code null} to clear).
   *
   * @param condition the new breakpoint condition
   */
  void setCondition(String condition);

  /**
   * Removes the breakpoint from the JS debugger and invokes the
   * callback once the operation has finished. This operation does not require
   * a flush() invocation.
   *
   * @param callback to invoke once the operation result is available
   */
  void clear(JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback);

  /**
   * Flushes the breakpoint parameter changes (set* methods) into the browser
   * and invokes the callback once the operation has finished. This method must
   * be called for the set* method invocations to take effect.
   *
   * @param callback to invoke once the operation result is available
   */
  void flush(JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback);


  /**
   * An abstraction over possible breakpoint targets: script name, script id etc.
   * This is essentially an Algebraic Type with several cases (see {@link Target.ScriptName},
   * {@link Target.ScriptId}). Some cases may be provided as extension
   * (see {@link BreakpointTypeExtension}).
   */
  abstract class Target {
    /**
     * Dispatches call on the actual Target type.
     * @param visitor user-provided {@link Visitor} that may also implement some additional
     *     interfaces (for extended types) that is checked on runtime
     */
    public abstract <R> R accept(Visitor<R> visitor);

    public interface Visitor<R> {
      R visitScriptName(String scriptName);
      R visitScriptId(long scriptId);
      R visitUnknown(Target target);
    }

    public static class ScriptName extends Target {
      private final String name;
      public ScriptName(String name) {
        this.name = name;
      }
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitScriptName(name);
      }
    }

    public static class ScriptId extends Target {
      private final long id;
      public ScriptId(long id) {
        this.id = id;
      }
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitScriptId(id);
      }
    }
  }
}
