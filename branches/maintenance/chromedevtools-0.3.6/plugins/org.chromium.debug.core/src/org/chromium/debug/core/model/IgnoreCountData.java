package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.Breakpoint;

/**
 * A structure used to store ignore count parameter. It contains integer 'value' and
 * additional field 'state' (see the enum).
 */
public class IgnoreCountData {
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

    IgnoreCountData.State state;
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
  private IgnoreCountData.State state;

  public IgnoreCountData(int value, IgnoreCountData.State state) {
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

  public IgnoreCountData.State getState() {
    return state;
  }
  public void setState(IgnoreCountData.State state) {
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