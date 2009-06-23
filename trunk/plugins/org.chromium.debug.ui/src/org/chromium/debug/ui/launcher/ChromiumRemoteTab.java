// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The "Remote" tab for the Chromium Javascript launch tab group.
 */
public class ChromiumRemoteTab extends AbstractLaunchConfigurationTab {

  private Text v8DebugPort;

  private Text v8ProjectName;

  @Override
  public void createControl(Composite parent) {
    Composite composite = createDefaultComposite(parent);
    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    };

    // Label for the port field
    Label portLabel = new Label(composite, SWT.NONE);
    portLabel.setText(Messages.ChromiumRemoteTab_PortLabel);

    // Port text field
    v8DebugPort = new Text(composite, SWT.SINGLE | SWT.BORDER);
    v8DebugPort.addModifyListener(modifyListener);

    // Label for the project name field
    Label projectLabel = new Label(composite, SWT.NONE);
    projectLabel.setText(Messages.ChromiumRemoteTab_ProjectNameLabel);

    // Project name text field
    v8ProjectName = new Text(composite, SWT.SINGLE | SWT.BORDER);
    v8ProjectName.addModifyListener(modifyListener);
  }

  @Override
  public String getName() {
    return Messages.ChromiumRemoteTab_RemoteTabName;
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    int debugPort = PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT);
    String projectName = PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_PROJECT_NAME);

    try {
      v8DebugPort.setText(Integer.toString(config.getAttribute(LaunchType.CHROMIUM_DEBUG_PORT,
          debugPort)));
      v8ProjectName.setText(config
          .getAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME, projectName));
    } catch (CoreException ce) {
      v8DebugPort.setText(Integer.toString(debugPort));
      v8ProjectName.setText(projectName);
    }
    v8DebugPort.setTextLimit(5);
    v8ProjectName.setTextLimit(50);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    try {
      config.setAttribute(LaunchType.CHROMIUM_DEBUG_PORT, Integer.parseInt(v8DebugPort.getText()
          .trim()));
    } catch (NumberFormatException e) {
      // fall through
    }
    config.setAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME, v8ProjectName.getText().trim());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    int port = PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT);
    config.setAttribute(LaunchType.CHROMIUM_DEBUG_PORT, port);

    String projectName = PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_PROJECT_NAME);
    projectName = getNewProjectName(projectName);
    config.setAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME, projectName);
  }

  private Composite createDefaultComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    setControl(composite);

    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    composite.setLayout(layout);

    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    composite.setLayoutData(data);

    return composite;
  }

  /**
   * Guarantees that the project name is unique for this workspace.
   *
   * @param baseProjectName the base project name
   * @return the {@code baseProjectName}, or that with a unique suffix appended
   */
  private String getNewProjectName(String baseProjectName) {
    String projName = baseProjectName;
    int counter = 1;

    while (!isProjectUnique(projName)) {
      projName = baseProjectName + "_" + (counter++); //$NON-NLS-1$
    }

    return projName;
  }

  private static boolean isProjectUnique(String projName) {
    return !ChromiumDebugPluginUtil.projectExists(projName);
  }
}
