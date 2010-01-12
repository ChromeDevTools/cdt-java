// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * The Chromium JavaScript debugger launch configuration tab group.
 */
public class LaunchTabGroup extends AbstractLaunchConfigurationTabGroup {
  public static class Chromium extends LaunchTabGroup {
  }

  public static class StandaloneV8 extends LaunchTabGroup {
  }

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    setTabs(new ILaunchConfigurationTab[] { new ChromiumRemoteTab(), new CommonTab() });
  }

}
