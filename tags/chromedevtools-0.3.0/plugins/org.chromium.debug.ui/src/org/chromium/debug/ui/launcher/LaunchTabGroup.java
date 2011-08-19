// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

/**
 * The Chromium JavaScript debugger launch configuration tab group.
 */
public abstract class LaunchTabGroup extends AbstractLaunchConfigurationTabGroup {
  public static class Chromium extends LaunchTabGroup {
    @Override protected ChromiumRemoteTab<?> createRemoteTab() {
      return new ChromiumRemoteTab.DevToolsProtocol();
    }
  }

  public static class StandaloneV8 extends LaunchTabGroup {
    @Override protected ChromiumRemoteTab<?> createRemoteTab() {
      return new ChromiumRemoteTab.Standalone();
    }
  }

  public static class Wip extends LaunchTabGroup {
    @Override protected ChromiumRemoteTab<?> createRemoteTab() {
      return new WipRemoteTab();
    }
  }

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    setTabs(new ILaunchConfigurationTab[] { createRemoteTab(),
        new SourceLookupTab(), new CommonTab() });
  }

  protected abstract ChromiumRemoteTab<?> createRemoteTab();
}
