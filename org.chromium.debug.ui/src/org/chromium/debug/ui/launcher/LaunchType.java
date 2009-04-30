// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * A launch configuration delegate for the Chromium debugging.
 */
public class LaunchType implements ILaunchConfigurationDelegate {
  // Launch configuration attributes.
  public static final String CHROMIUM_DEBUG_PORT = "debug_port"; //$NON-NLS-1$

  public static final String CHROMIUM_DEBUG_STARTUP_BREAK =
      "debug_startup_break"; //$NON-NLS-1$

  public static final String CHROMIUM_DEBUG_PROJECT_NAME = "debug_project_name"; //$NON-NLS-1$

  private static final String LOCALHOST = "127.0.0.1"; //$NON-NLS-1$

  public void launch(ILaunchConfiguration config, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    // Chromium launch is only supported for debugging.
    if (mode.equals(ILaunchManager.DEBUG_MODE)) {
      int port =
          config.getAttribute(LaunchType.CHROMIUM_DEBUG_PORT,
              PluginVariables.getValueAsInt(PluginVariables.DEFAULT_PORT));
      boolean bpOnStart =
          config.getAttribute(LaunchType.CHROMIUM_DEBUG_STARTUP_BREAK,
              Boolean.valueOf(PluginVariables.getValue(
                  PluginVariables.DEFAULT_BREAK_ON_STARTUP)));
      String projectName =
          config.getAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME,
              PluginVariables.getValue(PluginVariables.DEFAULT_PROJECT_NAME));

      IDebugTarget target =
            new DebugTargetImpl(launch, null, LOCALHOST, port, bpOnStart, projectName);
      launch.addDebugTarget(target);
    }
  }

}