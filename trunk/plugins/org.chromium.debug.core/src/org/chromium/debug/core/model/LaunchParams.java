// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
import org.chromium.debug.core.util.MementoFormat;
import org.chromium.debug.core.util.MementoFormat.ParserException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;

public class LaunchParams {

  /** Launch configuration attribute (debug host). */
  public static final String CHROMIUM_DEBUG_HOST = "debug_host"; //$NON-NLS-1$

  /** Launch configuration attribute (debug port). */
  public static final String CHROMIUM_DEBUG_PORT = "debug_port"; //$NON-NLS-1$

  public static final String ADD_NETWORK_CONSOLE = "add_network_console"; //$NON-NLS-1$

  public static final String BREAKPOINT_SYNC_DIRECTION =
      "breakpoint_startup_sync_direction"; //$NON-NLS-1$

  public static final String SOURCE_LOOKUP_MODE = "source_lookup_mode"; //$NON-NLS-1$

  public static final String WIP_BACKEND_ID = "wip_backend_id"; //$NON-NLS-1$

  public static final String PREDEFINED_SOURCE_WRAPPER_IDS =
      "predefined_source_wrapperd_ids"; //$NON-NLS-1$

  public enum LookupMode {
    EXACT_MATCH() {
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitExactMatch();
      }
    },
    AUTO_DETECT() {
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitAutoDetect();
      }
    };

    public abstract <R> R accept(Visitor<R> visitor);

    public interface Visitor<R> {
      R visitExactMatch();
      R visitAutoDetect();
    }

    public static final LookupMode DEFAULT_VALUE = EXACT_MATCH;

    public static final ValueConverter<String, LookupMode> STRING_CONVERTER =
        new ValueConverter<String, LookupMode>() {
      @Override
      public
      String encode(LookupMode logical) {
        return logical.toString();
      }

      @Override
      public
      LookupMode decode(String persistent) throws CoreException {
        try {
          return LookupMode.valueOf(persistent);
        } catch (IllegalArgumentException e) {
          Status status = new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
              "Failed to parse lookup mode value", e);
          throw new CoreException(status);
        }
      }
    };
  }

  public static abstract class ValueConverter<P, L> {
    public abstract P encode(L logical);
    public abstract L decode(P persistent) throws CoreException;

    @SuppressWarnings("unchecked")
    public
    static <T> ValueConverter<T, T> getTrivial() {
      return (Trivial<T>) Trivial.INSTANCE;
    }

    private static class Trivial<T> extends ValueConverter<T, T> {
      @Override public T encode(T logical) {
        return logical;
      }

      @Override public T decode(T persistent) {
        return persistent;
      }
      private static final Trivial<?> INSTANCE = new Trivial<Void>();
    }
  }


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

  public static final ValueConverter<String, List<String>>
      PREDEFINED_SOURCE_WRAPPER_IDS_CONVERTER =
      new ValueConverter<String, List<String>>() {
        @Override
        public String encode(List<String> logical) {
          StringBuilder builder = new StringBuilder();
          List<String> list = new ArrayList<String>(logical);
          Collections.sort(list);
          for (String id : list) {
            MementoFormat.encodeComponent(id, builder);
          }
          return builder.toString();
        }

        @Override
        public List<String> decode(String persistent) throws CoreException {
          List<String> result = new ArrayList<String>();
          MementoFormat.Parser parser = new MementoFormat.Parser(persistent);
          while (parser.hasMore()) {
            try {
              result.add(parser.nextComponent());
            } catch (ParserException e) {
              throw new RuntimeException("Failed to read config value '" + persistent + "'", e);
            }
          }
          return result;
        }
      };
}
