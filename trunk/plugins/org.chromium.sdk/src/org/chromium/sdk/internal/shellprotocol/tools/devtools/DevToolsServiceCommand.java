// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.shellprotocol.tools.devtools;

import java.util.HashMap;
import java.util.Map;

/**
 * Known DevToolsService tool commands.
 */
public enum DevToolsServiceCommand {
  PING("ping"),
  VERSION("version"),
  LIST_TABS("list_tabs"),
  ;

  private static Map<String, DevToolsServiceCommand> map =
      new HashMap<String, DevToolsServiceCommand>();

  static {
    for (DevToolsServiceCommand command : values()) {
      map.put(command.commandName, command);
    }
  }

  public final String commandName;

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
