// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Known Chromium tool names.
 */
public enum ToolName {

  DEVTOOLS_SERVICE("DevToolsService"), //$NON-NLS-1$
  V8_DEBUGGER("V8Debugger"), //$NON-NLS-1$
  ;

  private static final Map<String, ToolName> map =
      new HashMap<String, ToolName>();

  static {
    for (ToolName name : values()) {
      map.put(name.value, name);
    }
  }

  public static ToolName forString(String value) {
    if (value == null) {
      return null;
    }
    return map.get(value);
  }

  public String value;

  private ToolName(String value) {
    this.value = value;
  }
}
