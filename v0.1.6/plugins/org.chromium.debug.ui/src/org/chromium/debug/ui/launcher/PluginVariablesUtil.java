// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.ui.ChromiumDebugUIPlugin;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * Provides convenient access to the variables declared in the
 * org.eclipse.core.variables.valueVariables extension point.
 */
class PluginVariablesUtil {

  /** The default server port variable id. */
  public static final String DEFAULT_PORT =
      ChromiumDebugUIPlugin.PLUGIN_ID + ".chromium_debug_port"; //$NON-NLS-1$

  /**
   * @param variableName to get the value for
   * @return the variable value parsed as an integer
   * @throws NumberFormatException
   *           if the value cannot be parsed as an integer
   */
  public static int getValueAsInt(String variableName) {
    return Integer.parseInt(getValue(variableName));
  }

  /**
   * @param variableName to get the value for
   * @return the value of the specified variable
   */
  public static String getValue(String variableName) {
    return VariablesPlugin.getDefault().getStringVariableManager()
        .getValueVariable(variableName).getValue();
  }

  private PluginVariablesUtil() {
    // not instantiable
  }
}
