// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocol.data.BreakpointInfo;

/**
 * A generic implementation of the Breakpoint interface.
 */
public class BreakpointImpl implements Breakpoint {

  /**
   * The breakpoint type.
   */
  private final Type type;

  /**
   * The breakpoint id as reported by the JavaScript VM.
   */
  private long id;

  /**
   * The corresponding script name as reported by the JavaScript VM. May be null.
   */
  private String scriptName;

  /**
   * The corresponding script id as reported by the JavaScript VM. May be null.
   */
  private Long scriptId;

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

  public BreakpointImpl(Type type, long id, String scriptName, Long scriptId, long lineNumber,
      boolean enabled, int ignoreCount, String condition, BreakpointManager breakpointManager) {
    this.type = type;
    this.scriptName = scriptName;
    this.scriptId = scriptId;
    this.id = id;
    this.isEnabled = enabled;
    this.ignoreCount = ignoreCount;
    this.condition = condition;
    this.lineNumber = lineNumber;
    this.breakpointManager = breakpointManager;
  }

  public BreakpointImpl(BreakpointInfo info, BreakpointManager breakpointManager) {
    this.type = getType(info);
    this.id = info.number();
    this.breakpointManager = breakpointManager;
    updateFromRemote(info);
  }
  public void updateFromRemote(BreakpointInfo info) {
    if (this.type != getType(info)) {
      throw new IllegalArgumentException();
    }
    if (this.id != info.number()) {
      throw new IllegalArgumentException();
    }
    this.lineNumber = info.line();
    this.isEnabled = info.active();
    this.ignoreCount = (int) info.ignoreCount();
    this.condition = info.condition();
    this.scriptName = info.script_name();
    this.scriptId = info.script_id();
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public Type getType() {
    return type;
  }

  public long getId() {
    return id;
  }

  public String getScriptName() {
    return scriptName;
  }

  public Long getScriptId() {
    return scriptId;
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

  public void clear(JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback) {
    breakpointManager.clearBreakpoint(this, callback, syncCallback);
    // The order must be preserved, otherwise the breakpointProcessor will not be able
    // to identify the original breakpoint ID.
    this.id = INVALID_ID;
  }

  public void flush(final JavascriptVm.BreakpointCallback callback, SyncCallback syncCallback) {
    if (!isDirty()) {
      if (callback != null) {
        callback.success(this);
      }
      return;
    }
    breakpointManager.changeBreakpoint(this, callback, syncCallback);
    setDirty(false);
  }

  private void setDirty(boolean isDirty) {
    this.isDirty = isDirty;
  }

  private boolean isDirty() {
    return isDirty;
  }

  private static Type getType(BreakpointInfo info) {
    BreakpointInfo.Type infoType = info.type();
    switch (infoType) {
      case SCRIPTID: return Type.SCRIPT_ID;
      case SCRIPTNAME: return Type.SCRIPT_NAME;
      case FUNCTION: return Type.FUNCTION;
    }
    throw new RuntimeException("Unknown type: " + infoType);
  }
}
