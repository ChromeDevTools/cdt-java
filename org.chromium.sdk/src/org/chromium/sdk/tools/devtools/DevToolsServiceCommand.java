// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.devtools;

import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.tools.ToolName;

/**
 * Known DevToolsService tool commands.
 */
public enum DevToolsServiceCommand {
  PING("ping"), //$NON-NLS-1$
  VERSION("version"), //$NON-NLS-1$
  LIST_TABS("list_tabs"), //$NON-NLS-1$
  ;

  private static Map<String, DevToolsServiceCommand> map =
      new HashMap<String, DevToolsServiceCommand>();

  static {
    for (DevToolsServiceCommand command : values()) {
      map.put(command.commandName(), command);
    }
  }

  private String commandName;

  public String commandName() {
    return commandName;
  }

  public String toolName() {
    return ToolName.DEVTOOLS_SERVICE.value;
  }

  private DevToolsServiceCommand(String commandName) {
    this.commandName = commandName;
  }

  public static DevToolsServiceCommand forString(String commandName) {
    if (commandName == null) {
      return null;
    }
    return map.get(commandName);
  }
}
