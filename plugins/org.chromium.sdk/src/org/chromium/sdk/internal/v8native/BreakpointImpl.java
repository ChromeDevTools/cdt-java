// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.protocol.input.data.BreakpointInfo;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * A generic implementation of the Breakpoint interface.
 */
public class BreakpointImpl implements Breakpoint {

  /**
   * The breakpoint target.
   */
  private Target target;

  /**
   * The breakpoint id as reported by the JavaScript VM.
   */
  private long id;

  /**
   * Breakpoint line number. May become invalidated by LiveEdit actions.
   */
  private long lineNumber;

  /**
   * Whether the breakpoint is enabled.
   */
  private boolean isEnabled;

  /**
   * The number of times the breakpoint should be ignored
   * by the JavaScript VM until it fires.
   */
  private int ignoreCount;

  /**
   * The breakpoint condition (plain JavaScript) that should be {@code true}
   * for the breakpoint to fire.
   */
  private String condition;

  /**
   * The breakpoint manager that manages this breakpoint.
   */
  private final BreakpointManager breakpointManager;

  /**
   * Whether the breakpoint data have changed with respect
   * to the JavaScript VM data.
   */
  private volatile boolean isDirty = false;

  public BreakpointImpl(long id, Target target, long lineNumber,
      boolean enabled, int ignoreCount, String condition, BreakpointManager breakpointManager) {
    this.target = target;
    this.id = id;
    this.isEnabled = enabled;
    this.ignoreCount = ignoreCount;
    this.condition = condition;
    this.lineNumber = lineNumber;
    this.breakpointManager = breakpointManager;
  }

  public BreakpointImpl(BreakpointInfo info, BreakpointManager breakpointManager) {
    this.target = getType(info);
    this.id = info.number();
    this.breakpointManager = breakpointManager;
    updateFromRemote(info);
  }
  public void updateFromRemote(BreakpointInfo info) {
    if (this.id != info.number()) {
      throw new IllegalArgumentException();
    }
    this.lineNumber = info.line();
    this.isEnabled = info.active();
    this.ignoreCount = (int) info.ignoreCount();
    this.condition = info.condition();
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public Target getTarget() {
    return target;
  }

  public long getId() {
    return id;
  }

  public int getIgnoreCount() {
    return ignoreCount;
  }

  public String getCondition() {
    return condition;
  }

  public long getLineNumber() {
    return lineNumber;
  }

  public void setEnabled(boolean enabled) {
    if (this.isEnabled != enabled) {
      setDirty(true);
    }
    this.isEnabled = enabled;
  }

  public void setIgnoreCount(int ignoreCount) {
    if (this.ignoreCount != ignoreCount) {
      setDirty(true);
    }
    this.ignoreCount = ignoreCount;
  }


  public void setCondition(String condition) {
    if (!eq(this.condition, condition)) {
      setDirty(true);
    }
    this.condition = condition;
  }

  private static <T> boolean eq(T left, T right) {
    return left == right || (left != null && left.equals(right));
  }

  @Override
  public RelayOk clear(JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback) {
    // TODO: make this code thread-safe.
    long originalId = this.id;
    this.id = INVALID_ID;
    return breakpointManager.clearBreakpoint(this, callback, syncCallback, originalId);
  }

  @Override
  public RelayOk flush(final JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback) {
    if (!isDirty()) {
      if (callback != null) {
        callback.success(this);
      }
      return RelaySyncCallback.finish(syncCallback);
    }
    setDirty(false);
    return breakpointManager.changeBreakpoint(this, callback, syncCallback);
  }

  private void setDirty(boolean isDirty) {
    this.isDirty = isDirty;
  }

  private boolean isDirty() {
    return isDirty;
  }

  private static Target getType(BreakpointInfo info) {
    BreakpointInfo.Type infoType = info.type();
    switch (infoType) {
      case SCRIPTID: return new Target.ScriptId(info.script_id());
      case SCRIPTNAME: return new Target.ScriptName(info.script_name());
      case SCRIPTREGEXP: return new ScriptRegExpTarget(info.script_name());
      case FUNCTION: return new FunctionTarget(null);
    }
    throw new RuntimeException("Unknown type: " + infoType);
  }

  /**
   * Visitor interface that includes all extensions.
   */
  public interface TargetExtendedVisitor<R> extends
      BreakpointTypeExtension.FunctionSupport.Visitor<R>,
      BreakpointTypeExtension.ScriptRegExpSupport.Visitor<R> {
  }

  static class ScriptRegExpTarget extends Target {
    private final String regExp;

    ScriptRegExpTarget(String regExp) {
      this.regExp = regExp;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      if (visitor instanceof BreakpointTypeExtension.ScriptRegExpSupport.Visitor) {
        BreakpointTypeExtension.ScriptRegExpSupport.Visitor<R> regExpVisitor =
            (BreakpointTypeExtension.ScriptRegExpSupport.Visitor<R>) visitor;
        return regExpVisitor.visitRegExp(regExp);
      } else {
        return visitor.visitUnknown(this);
      }
    }
  }

  static class FunctionTarget extends Target {
    private final String expression;
    FunctionTarget(String expression) {
      this.expression = expression;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      if (visitor instanceof BreakpointTypeExtension.FunctionSupport.Visitor) {
        BreakpointTypeExtension.FunctionSupport.Visitor<R> functionVisitor =
            (BreakpointTypeExtension.FunctionSupport.Visitor<R>) visitor;
        return functionVisitor.visitFunction(expression);
      } else {
        return visitor.visitUnknown(this);
      }
    }
  }
}
