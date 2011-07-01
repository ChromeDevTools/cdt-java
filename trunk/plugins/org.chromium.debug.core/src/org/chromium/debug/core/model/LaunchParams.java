// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Arrays;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

public class LaunchParams {

  /** Launch configuration attribute (debug host). */
  public static final String CHROMIUM_DEBUG_HOST = "debug_host"; //$NON-NLS-1$

  /** Launch configuration attribute (debug port). */
  public static final String CHROMIUM_DEBUG_PORT = "debug_port"; //$NON-NLS-1$

  public static final String ADD_NETWORK_CONSOLE = "add_network_console"; //$NON-NLS-1$

  public static final String BREAKPOINT_SYNC_DIRECTION =
      "breakpoint_startup_sync_direction"; //$NON-NLS-1$

  public static final String INACCURATE_SOURCE_LOOKUP = "inaccurate_source_lookup"; //$NON-NLS-1$


  public static class BreakpointOption {
    private final String label;
    private final Direction direction;

    BreakpointOption(String label, Direction direction) {
      this.label = label;
      this.direction = direction;
    }

    public Direction getDirection() {
      return direction;
    }

    public String getDirectionStringValue() {
      if (direction == null) {
        return "";
      } else {
        return direction.toString();
      }
    }

    public String getLabel() {
      return label;
    }
  }

  public static Direction readBreakpointSyncDirection(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    String breakpointOptionString =
        launchConfiguration.getAttribute(BREAKPOINT_SYNC_DIRECTION, (String)null);
    int optionIndex = findBreakpointOption(breakpointOptionString);
    return BREAKPOINT_OPTIONS.get(optionIndex).getDirection();
  }

  public final static List<? extends BreakpointOption> BREAKPOINT_OPTIONS = Arrays.asList(
      new BreakpointOption(Messages.LaunchParams_MERGE_OPTION, Direction.MERGE),
      new BreakpointOption(Messages.LaunchParams_RESET_REMOTE_OPTION,
          Direction.RESET_REMOTE),
      new BreakpointOption(Messages.LaunchParams_NONE_OPTION, null));

  public static int findBreakpointOption(String optionText) {
    int res;
    res = findBreakpointOptionRaw(optionText);
    if (res != -1) {
      return res;
    }
    res = findBreakpointOptionRaw(null);
    if (res != -1) {
      return res;
    }
    throw new RuntimeException("Failed to find breakpoint option"); //$NON-NLS-1$
  }

  private static int findBreakpointOptionRaw(String optionText) {
    Direction direction;
    if (optionText == null || optionText.length() == 0) {
      direction = null;
    } else {
      try {
        direction = Direction.valueOf(optionText);
      } catch (IllegalArgumentException e) {
        ChromiumDebugPlugin.log(
            new Exception("Failed to parse breakpoint synchronization option", e)); //$NON-NLS-1$
        return -1;
      }
    }
    for (int i = 0; i < BREAKPOINT_OPTIONS.size(); i++) {
      if (BREAKPOINT_OPTIONS.get(i).getDirection() == direction) {
        return i;
      }
    }
    return -1;
  }
}
