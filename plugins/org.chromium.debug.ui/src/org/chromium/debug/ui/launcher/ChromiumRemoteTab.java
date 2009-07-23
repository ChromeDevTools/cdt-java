// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * The "Remote" tab for the Chromium JavaScript launch tab group.
 */
public class ChromiumRemoteTab extends AbstractLaunchConfigurationTab {

  private static final String PORT_FIELD_NAME = "port_field"; //$NON-NLS-1$
  private static final String PROJECT_NAME_FIELD_NAME = "project_name_field"; //$NON-NLS-1$
  private static final String ADD_NETWORK_CONSOLE_FIELD_NAME =
      "add_network_console_field"; //$NON-NLS-1$
  
   // However, recommended range is [1024, 32767].
  private static final int minimumPortValue = 0;
  private static final int maximumPortValue = 65535;
  
  private IntegerFieldEditor debugPort;
  private StringFieldEditor projectName;
  private BooleanFieldEditor addNetworkConsole;
  private final PreferenceStore store = new PreferenceStore();

  public void createControl(Composite parent) {
    Composite composite = createDefaultComposite(parent);

    IPropertyChangeListener modifyListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        updateLaunchConfigurationDialog();
      }
    };

    Composite propertiesComp = createInnerComposite(composite, 2);
    // Port text field
    debugPort = new IntegerFieldEditor(PORT_FIELD_NAME, Messages.ChromiumRemoteTab_PortLabel,
        propertiesComp);
    debugPort.setPropertyChangeListener(modifyListener);
    debugPort.setPreferenceStore(store);

    // Project name text field
    projectName = new StringFieldEditor(PROJECT_NAME_FIELD_NAME,
        Messages.ChromiumRemoteTab_ProjectNameLabel, propertiesComp);
    projectName.setPropertyChangeListener(modifyListener);
    projectName.setPreferenceStore(store);
    projectName.setTextLimit(50);
    
    addNetworkConsole = new BooleanFieldEditor(ADD_NETWORK_CONSOLE_FIELD_NAME,
        Messages.ChromiumRemoteTab_ShowDebuggerNetworkCommunication,
        propertiesComp);
    addNetworkConsole.setPreferenceStore(store);
    addNetworkConsole.setPropertyChangeListener(modifyListener);
  }

  public String getName() {
    return Messages.ChromiumRemoteTab_RemoteTabName;
  }

  public void initializeFrom(ILaunchConfiguration config) {
    int debugPortDefault = PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT);
    String projectNameDefault =
        PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_PROJECT_NAME);
    
    try {
      store.setDefault(PORT_FIELD_NAME, config.getAttribute(LaunchType.CHROMIUM_DEBUG_PORT,
          debugPortDefault));
      store.setDefault(PROJECT_NAME_FIELD_NAME, config.getAttribute(
          LaunchType.CHROMIUM_DEBUG_PROJECT_NAME, projectNameDefault));
      store.setDefault(ADD_NETWORK_CONSOLE_FIELD_NAME, config.getAttribute(
          LaunchType.ADD_NETWORK_CONSOLE, false));
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(new Exception("Unexpected storage problem", e)); //$NON-NLS-1$
      store.setDefault(PORT_FIELD_NAME, debugPortDefault);
      store.setDefault(PROJECT_NAME_FIELD_NAME, projectNameDefault);
      store.setDefault(ADD_NETWORK_CONSOLE_FIELD_NAME, false);
    }

    debugPort.loadDefault();
    projectName.loadDefault();
    addNetworkConsole.loadDefault();
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    storeEditor(debugPort, "-1"); //$NON-NLS-1$
    storeEditor(projectName, ""); //$NON-NLS-1$
    storeEditor(addNetworkConsole, ""); //$NON-NLS-1$

    config.setAttribute(LaunchType.CHROMIUM_DEBUG_PORT, store.getInt(PORT_FIELD_NAME));
    config.setAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME,
        store.getString(PROJECT_NAME_FIELD_NAME).trim());
    config.setAttribute(LaunchType.ADD_NETWORK_CONSOLE,
        store.getBoolean(ADD_NETWORK_CONSOLE_FIELD_NAME));
  }
  
  @Override
  public boolean isValid(ILaunchConfiguration config) {
    try {
      int port = config.getAttribute(LaunchType.CHROMIUM_DEBUG_PORT, -1);
      if (port < minimumPortValue || port > maximumPortValue) {
        setErrorMessage(Messages.ChromiumRemoteTab_InvalidPortNumberError);
        return false;
      }
      String projectName = config.getAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME,
          ""); //$NON-NLS-1$
      if (!ResourcesPlugin.getWorkspace().validateName(projectName, IResource.PROJECT).isOK()) {
        setErrorMessage(Messages.ChromiumRemoteTab_InvalidProjectNameError); 
        return false;
      }
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(new Exception("Unexpected storage problem", e)); //$NON-NLS-1$
    }
    
    setErrorMessage(null); 
    return true;
  }


  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    int port = PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT);
    config.setAttribute(LaunchType.CHROMIUM_DEBUG_PORT, port);

    String projectName = PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_PROJECT_NAME);
    projectName = getNewProjectName(projectName);
    config.setAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME, projectName);
  }
  
  @Override
  public Image getImage() {
    return DebugUITools.getImage(IDebugUIConstants.IMG_LCL_DISCONNECT);
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

  private Composite createInnerComposite(Composite parent, int numColumns) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(numColumns, false));
    GridData gd = new GridData(GridData.FILL_BOTH);
    composite.setLayoutData(gd);
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
  
  private static void storeEditor(FieldEditor editor, String errorValue) {
    if (editor.isValid()) {
      editor.store();
    } else {
      editor.getPreferenceStore().setValue(editor.getPreferenceName(), errorValue);
    }
  }
}
