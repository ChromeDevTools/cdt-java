// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.ui.ChromiumDebugUIPlugin;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * Provides convenient access to the variables declared in the
 * org.eclipse.core.variables.valueVariables extension point.
 */
public class PluginVariables {
  public static final String DEFAULT_PORT =
      ChromiumDebugUIPlugin.PLUGIN_ID + ".chromium_debug_port"; //$NON-NLS-1$

  public static final String DEFAULT_BREAK_ON_STARTUP =
      ChromiumDebugUIPlugin.PLUGIN_ID + ".chromium_break_on_startup"; //$NON-NLS-1$

  public static final String DEFAULT_PROJECT_NAME =
      ChromiumDebugUIPlugin.PLUGIN_ID + ".chromium_project_name"; //$NON-NLS-1$

  /**
   * @return the variable value parsed as an integer.
   * @throws NumberFormatException
   *           if the value cannot be parsed as an integer.
   */
  public static final int getValueAsInt(String variableName) {
    return Integer.parseInt(getValue(variableName));
  }

  /**
   * @return the value of the specified variable.
   */
  public static final String getValue(String varName) {
    IStringVariableManager varMgr =
        VariablesPlugin.getDefault().getStringVariableManager();
    return varMgr.getValueVariable(varName).getValue();
  }
}
