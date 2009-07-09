// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "setbreakpoint" V8 request message.
 */
public class SetBreakpointMessage extends DebuggerMessage {

  private static final Map<Breakpoint.Type, String> typeToV8Type =
      new HashMap<Breakpoint.Type, String>();

  static {
    typeToV8Type.put(Breakpoint.Type.FUNCTION, "function");
    typeToV8Type.put(Breakpoint.Type.SCRIPT_NAME, "script");
    typeToV8Type.put(Breakpoint.Type.SCRIPT_ID, "scriptId");
  }

  /**
   * @param type ("function", "handle", or "script")
   * @param target function expression, script identification, or handle decimal number
   * @param line in the script or function
   * @param position of the target start within the line
   * @param enabled whether the breakpoint is enabled initially. Nullable, default is true
   * @param condition nullable string with breakpoint condition
   * @param ignoreCount nullable number specifying the amount of break point hits to ignore.
   *        Default is 0
   */
  public SetBreakpointMessage(Breakpoint.Type type, String target,
      Integer line, Integer position, Boolean enabled, String condition,
      Integer ignoreCount) {
    super(DebuggerCommand.SETBREAKPOINT.value);
    putArgument("type", typeToV8Type.get(type));
    putArgument("target", target);
    putArgument("line", line);
    putArgument("position", position);
    putArgument("enabled", enabled);
    putArgument("condition", condition);
    putArgument("ignoreCount", ignoreCount);
  }
}
