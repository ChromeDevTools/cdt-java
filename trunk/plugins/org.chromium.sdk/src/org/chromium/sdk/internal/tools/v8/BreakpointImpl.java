// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.internal.tools.v8.processor.BreakpointProcessor;

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
   * The breakpoint processor that performs JavaScript VM communications.
   */
  private final BreakpointProcessor breakpointProcessor;

  /**
   * Whether the breakpoint data have changed with respect
   * to the JavaScript VM data.
   */
  private volatile boolean isDirty = false;

  public BreakpointImpl(Type type, long id, boolean enabled, int ignoreCount, String condition,
      BreakpointProcessor breakpointProcessor) {
    this.type = type;
    this.id = id;
    this.isEnabled = enabled;
    this.ignoreCount = ignoreCount;
    this.condition = condition;
    this.breakpointProcessor = breakpointProcessor;
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public int getIgnoreCount() {
    return ignoreCount;
  }

  @Override
  public String getCondition() {
    return condition;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (this.isEnabled != enabled) {
      setDirty(true);
    }
    this.isEnabled = enabled;
  }

  @Override
  public void setIgnoreCount(int ignoreCount) {
    if (this.ignoreCount != ignoreCount) {
      setDirty(true);
    }
    this.ignoreCount = ignoreCount;
  }


  @Override
  public void setCondition(String condition) {
    if (!eq(this.condition, condition)) {
      setDirty(true);
    }
    this.condition = condition;
  }

  private boolean eq(Object left, Object right) {
    return left == right || (left != null && left.equals(right));
  }

  @Override
  public void clear(final BrowserTab.BreakpointCallback callback) {
    breakpointProcessor.clearBreakpoint(this, callback);
    // The order must be preserved, otherwise the breakpointProcessor will not be able
    // to identify the original breakpoint ID.
    this.id = INVALID_ID;
  }

  @Override
  public void flush(final BrowserTab.BreakpointCallback callback) {
    if (!isDirty()) {
      if (callback != null) {
        callback.success(this);
      }
      return;
    }
    breakpointProcessor.changeBreakpoint(this, callback);
    setDirty(false);
  }

  private void setDirty(boolean isDirty) {
    this.isDirty = isDirty;
  }

  private boolean isDirty() {
    return isDirty;
  }

}