// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Set;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.Breakpoint;
import org.eclipse.core.resources.IMarkerDelta;
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

  /**
   * @return ignore count number or {@link Breakpoint#EMPTY_VALUE}
   */
  public int getEffectiveIgnoreCount() {
    IgnoreCountData data = getIgnoreCountData();
    return data.getEffectiveValue();
  }

  /**
   * Resets ignore count so that it's effective value becomes {@link Breakpoint#EMPTY_VALUE},
   * but the change does not cause update back to remote VM.
   */
  public void silentlyResetIgnoreCount() throws CoreException {
    IgnoreCountData data = getIgnoreCountData();
    if (data.getState() == IgnoreCountData.State.RESET || data.getValue() <= 0) {
      return;
    }
    data.setState(IgnoreCountData.State.RESET);
    setIgnoreCountData(data);
  }

  public abstract IgnoreCountData getIgnoreCountData();
  public abstract void setIgnoreCountData(IgnoreCountData data) throws CoreException;

  /**
   * Returns set of properties that have changed comparing to the state saved in delta parameter.
   * Doesn't enumerate properties that were changed 'silently'
   * (see {@link #silentlyResetIgnoreCount()}).
   */
  public abstract Set<MutableProperty> getChangedProperty(IMarkerDelta delta);

  public enum MutableProperty {
    ENABLED, CONDITION, IGNORE_COUNT
  }

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

  /**
   * A structure used to store ignore count parameter. It contains integer 'value' and
   * additional field 'state' (see the enum).
   */
  public static class IgnoreCountData {
    public static IgnoreCountData parseString(String input) {
      int separatorPos = input.indexOf('/');
      String valueStr;
      String stateStr;
      if (separatorPos == -1) {
        valueStr = input;
        stateStr = null;
      } else {
        valueStr = input.substring(0, separatorPos);
        stateStr = input.substring(separatorPos + 1);
      }
      int value;
      if (valueStr.isEmpty()) {
        value = Breakpoint.EMPTY_VALUE;
      } else {
        try {
          value = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
          ChromiumDebugPlugin.log(new Exception("Failed to parse ignore count value: " + input, e));
          value = Breakpoint.EMPTY_VALUE;
        }
      }

      State state;
      if (stateStr == null) {
        if (value == Breakpoint.EMPTY_VALUE) {
          state = State.DISABLED;
        } else {
          state = State.ENABLED;
        }
      } else {
        try {
          state = State.valueOf(stateStr);
        } catch (IllegalArgumentException e) {
          ChromiumDebugPlugin.log(new Exception("Failed to parse ignore count value: " + input, e));
          state = State.ENABLED;
        }
      }
      return new IgnoreCountData(value, state);
    }

    private int value;
    private State state;

    public IgnoreCountData(int value, State state) {
      this.value = value;
      this.state = state;
    }
    public int getValue() {
      return value;
    }
    public void setValue(int value) {
      this.value = value;
    }

    public int getEffectiveValue() {
      if (state == IgnoreCountData.State.ENABLED) {
        return value;
      } else {
        return Breakpoint.EMPTY_VALUE;
      }
    }

    public State getState() {
      return state;
    }
    public void setState(State state) {
      this.state = state;
    }
    public String getStringRepresentation() {
      return value + "/" + state.toString();
    }

    /**
     * Additional field of the ignore count data. It modulates semantics of the numeric 'value'.
     */
    public enum State {
      /**
       * User has set ignore count for the breakpoint, value stores the number.
       */
      ENABLED,

      /**
       * User has disabled ignore count, value stores last used number and is visible
       * in 'properties' dialog.
       */
      DISABLED,

      /**
       * IDE has disabled ignore count because debugger finally has stopped on the breakpoint.
       * This differs from {@link #DISABLED} only to give breakpoint handler a hint that this
       * change mustn't be applied to SDK.
       */
      RESET
    }
  }
}
