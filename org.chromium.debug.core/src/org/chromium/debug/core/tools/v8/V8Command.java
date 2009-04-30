// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8;

import java.util.HashMap;
import java.util.Map;

/**
 * Known V8 VM debugger commands and events.
 */
public enum V8Command {
  CONTINUE("continue"), //$NON-NLS-1$
  EVALUATE("evaluate"), //$NON-NLS-1$
  BACKTRACE("backtrace"), //$NON-NLS-1$
  FRAME("frame"), //$NON-NLS-1$
  SCRIPTS("scripts"), //$NON-NLS-1$
  SOURCE("source"), //$NON-NLS-1$
  SETBREAKPOINT("setbreakpoint"), //$NON-NLS-1$
  CHANGEBREAKPOINT("changebreakpoint"), //$NON-NLS-1$
  CLEARBREAKPOINT("clearbreakpoint"), //$NON-NLS-1$

  // Event
  BREAK("break"), //$NON-NLS-1$
  EXCEPTION("exception"), //$NON-NLS-1$
  ;

  public String value;

  V8Command(String value) {
    this.value = value;
  }

  private static final Map<String, V8Command> valueToCommandMap =
      new HashMap<String, V8Command>();

  static {
    for (V8Command c : values()) {
      valueToCommandMap.put(c.value, c);
    }
  }

  /**
   * @param value the V8Command string value
   * @return the V8Command instance or null if none corresponds to value
   */
  public static V8Command forString(String value) {
    if (value == null) {
      return null;
    }
    return valueToCommandMap.get(value);
  }

}
