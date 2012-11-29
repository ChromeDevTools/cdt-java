// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;

import org.chromium.debug.ui.launcher.ChromiumRemoteTab.HostChecker;
import org.chromium.sdk.util.BasicUtil;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

/**
 * The Chromium JavaScript debugger launch configuration tab group.
 */
public abstract class LaunchTabGroup extends AbstractLaunchConfigurationTabGroup {
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
    setTabs(BasicUtil.toArray(createTabList(dialog, mode), ILaunchConfigurationTab.class));
  }

  protected ArrayList<ILaunchConfigurationTab> createTabList(ILaunchConfigurationDialog dialog,
      String mode) {
    ArrayList<ILaunchConfigurationTab> result = new ArrayList<ILaunchConfigurationTab>(4);
    ChromiumRemoteTab<?> remoteTab = createRemoteTab();
    result.add(remoteTab);
    result.add(new SourceLookupTab());
    result.add(new ScriptMappingTab(remoteTab.getParams()));
    result.add(new CommonTab());
    return result;
  }

  protected abstract ChromiumRemoteTab<?> createRemoteTab();

  static class Params {
    private final HostChecker hostChecker;
    private final String scriptNameDescription;
    private final boolean enableSourceWrapper;

    Params(HostChecker hostChecker, String scriptNameDescription,
        boolean enableSourceWrapper) {
      this.hostChecker = hostChecker;
      this.scriptNameDescription = scriptNameDescription;
      this.enableSourceWrapper = enableSourceWrapper;
    }

    HostChecker getHostChecker() {
      return hostChecker;
    }

    String getScriptNameDescription() {
      return scriptNameDescription;
    }

    boolean preEnableSourceWrapper() {
      return enableSourceWrapper;
    }
  }
}
