// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.HashMap;
import java.util.Map;

/**
 * Known V8Debugger tool commands.
 */
public enum DebuggerToolCommand {
  ATTACH("attach"),
  DETACH("detach"),
  DEBUGGER_COMMAND("debugger_command"),
  EVALUATE_JAVASCRIPT("evaluate_javascript"),

  // Events
  NAVIGATED("navigated"),
  CLOSED("closed");

  private static final Map<String, DebuggerToolCommand> map =
      new HashMap<String, DebuggerToolCommand>();

  static {
    for (DebuggerToolCommand command : values()) {
      map.put(command.commandName, command);
    }
  }

  public String commandName;

  private DebuggerToolCommand(String value) {
    this.commandName = value;
  }

  public static DebuggerToolCommand forName(String name) {
    if (name == null) {
      return null;
    }
    return map.get(name);
  }
}
