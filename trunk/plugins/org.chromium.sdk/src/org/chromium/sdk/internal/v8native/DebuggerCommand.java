// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.HashMap;
import java.util.Map;

/**
 * Known V8 VM debugger commands and events.
 */
public enum DebuggerCommand {
  CONTINUE("continue"),
  EVALUATE("evaluate"),
  BACKTRACE("backtrace"),
  FRAME("frame"),
  SCRIPTS("scripts"),
  CHANGELIVE("changelive"),
  RESTARTFRAME("restartframe"),
  SOURCE("source"),
  SCOPE("scope"),
  SETBREAKPOINT("setbreakpoint"),
  CHANGEBREAKPOINT("changebreakpoint"),
  CLEARBREAKPOINT("clearbreakpoint"),
  LISTBREAKPOINTS("listbreakpoints"),
  LOOKUP("lookup"),
  SUSPEND("suspend"),
  VERSION("version"),
  FLAGS("flags"),

  // Events
  BREAK("break"),
  EXCEPTION("exception"),
  AFTER_COMPILE("afterCompile"),
  SCRIPT_COLLECTED("scriptCollected"),
  ;

  public final String value;

  DebuggerCommand(String value) {
    this.value = value;
  }

  private static final Map<String, DebuggerCommand> valueToCommandMap =
      new HashMap<String, DebuggerCommand>();

  static {
    for (DebuggerCommand c : values()) {
      valueToCommandMap.put(c.value, c);
    }
  }

  /**
   * @param value the DebuggerCommand string value
   * @return the DebuggerCommand instance or null if none corresponds to value
   */
  public static DebuggerCommand forString(String value) {
    if (value == null) {
      return null;
    }
    return valueToCommandMap.get(value);
  }

}
