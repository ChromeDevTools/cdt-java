// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * The "Remote" tab for the Chromium JavaScript launch tab group.
 */
public class ChromiumRemoteTab extends AbstractLaunchConfigurationTab {

  private static final String PORT_FIELD_NAME = "port_field"; //$NON-NLS-1$
  private static final String ADD_NETWORK_CONSOLE_FIELD_NAME =
      "add_network_console_field"; //$NON-NLS-1$

   // However, recommended range is [1024, 32767].
  private static final int minimumPortValue = 0;
  private static final int maximumPortValue = 65535;

  private IntegerFieldEditor debugPort;
  private BooleanFieldEditor addNetworkConsole;
  private RadioButtonsLogic breakpointRadioButtons;
  private final PreferenceStore store = new PreferenceStore();

  public void createControl(Composite parent) {
    Composite composite = createDefaultComposite(parent);

    IPropertyChangeListener modifyListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        updateLaunchConfigurationDialog();
      }
    };

    Group connectionGroup = new Group(composite, 0);
    connectionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    connectionGroup.setText(Messages.ChromiumRemoteTab_CONNECTION_GROUP);
    connectionGroup.setLayout(new GridLayout(1, false));

    Composite propertiesComp = createInnerComposite(connectionGroup, 2);

    // Port text field
    debugPort = new IntegerFieldEditor(PORT_FIELD_NAME, Messages.ChromiumRemoteTab_PortLabel,
        propertiesComp);
    debugPort.setPropertyChangeListener(modifyListener);
    debugPort.setPreferenceStore(store);

    addNetworkConsole = new BooleanFieldEditor(ADD_NETWORK_CONSOLE_FIELD_NAME,
        Messages.ChromiumRemoteTab_ShowDebuggerNetworkCommunication,
        propertiesComp);
    addNetworkConsole.setPreferenceStore(store);
    addNetworkConsole.setPropertyChangeListener(modifyListener);

    Group breakpointGroup = new Group(composite, 0);
    breakpointGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    breakpointGroup.setText(Messages.ChromiumRemoteTab_BREAKPOINT_GROUP);
    breakpointGroup.setLayout(new GridLayout(1, false));

    List<Button> radioButtons = new ArrayList<Button>();
    for (LaunchParams.BreakpointOption option : LaunchParams.BREAKPOINT_OPTIONS) {
      Button button = new Button(breakpointGroup, SWT.RADIO);
      button.setFont(parent.getFont());
      button.setText(option.getLabel());
      GridData gd = new GridData();
      button.setLayoutData(gd);
      SWTFactory.setButtonDimensionHint(button);
      radioButtons.add(button);
    }

    RadioButtonsLogic.Listener radioButtonsListener =
        new RadioButtonsLogic.Listener() {
          public void selectionChanged() {
            updateLaunchConfigurationDialog();
          }
        };
    breakpointRadioButtons = new RadioButtonsLogic(radioButtons, radioButtonsListener);
  }

  public String getName() {
    return Messages.ChromiumRemoteTab_RemoteTabName;
  }

  public void initializeFrom(ILaunchConfiguration config) {
    int debugPortDefault = PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT);

    String breakpointOptionStr;
    String defaultBreakpointOptionStr = Direction.MERGE.toString();
    try {
      store.setDefault(PORT_FIELD_NAME, config.getAttribute(LaunchParams.CHROMIUM_DEBUG_PORT,
          debugPortDefault));
      store.setDefault(ADD_NETWORK_CONSOLE_FIELD_NAME, config.getAttribute(
          LaunchParams.ADD_NETWORK_CONSOLE, false));
      breakpointOptionStr =
          config.getAttribute(LaunchParams.BREAKPOINT_SYNC_DIRECTION, defaultBreakpointOptionStr);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(new Exception("Unexpected storage problem", e)); //$NON-NLS-1$
      store.setDefault(PORT_FIELD_NAME, debugPortDefault);
      store.setDefault(ADD_NETWORK_CONSOLE_FIELD_NAME, false);
      breakpointOptionStr = defaultBreakpointOptionStr;
    }

    debugPort.loadDefault();
    addNetworkConsole.loadDefault();

    int breakpointOptionIndex = LaunchParams.findBreakpointOption(breakpointOptionStr);
    breakpointRadioButtons.select(breakpointOptionIndex);
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    storeEditor(debugPort, "-1"); //$NON-NLS-1$
    storeEditor(addNetworkConsole, ""); //$NON-NLS-1$

    config.setAttribute(LaunchParams.CHROMIUM_DEBUG_PORT, store.getInt(PORT_FIELD_NAME));
    config.setAttribute(LaunchParams.ADD_NETWORK_CONSOLE,
        store.getBoolean(ADD_NETWORK_CONSOLE_FIELD_NAME));
    int breakpointOption = breakpointRadioButtons.getSelected();
    config.setAttribute(LaunchParams.BREAKPOINT_SYNC_DIRECTION,
        LaunchParams.BREAKPOINT_OPTIONS.get(breakpointOption).getDirectionStringValue());
  }

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    try {
      int port = config.getAttribute(LaunchParams.CHROMIUM_DEBUG_PORT, -1);
      if (port < minimumPortValue || port > maximumPortValue) {
        setErrorMessage(Messages.ChromiumRemoteTab_InvalidPortNumberError);
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
    config.setAttribute(LaunchParams.CHROMIUM_DEBUG_PORT, port);
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
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    composite.setLayoutData(gd);
    return composite;
  }

  private static void storeEditor(FieldEditor editor, String errorValue) {
    if (editor.isValid()) {
      editor.store();
    } else {
      editor.getPreferenceStore().setValue(editor.getPreferenceName(), errorValue);
    }
  }

  private static class RadioButtonsLogic {
    private final List<Button> buttons;
    RadioButtonsLogic(List<Button> buttons, final Listener listener) {
      this.buttons = buttons;
      SelectionListener selectionListener = new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
        }
        public void widgetSelected(SelectionEvent e) {
          if (listener != null && e.widget instanceof Button) {
            Button button = (Button) e.widget;
            if (button.getSelection()) {
              listener.selectionChanged();
            }
          }
        }
      };

      for (Button button : buttons) {
        button.addSelectionListener(selectionListener);
      }
    }
    void select(int index) {
      for (int i = 0; i < buttons.size(); i++) {
        buttons.get(i).setSelection(i == index);
      }
    }
    int getSelected() {
      for (int i = 0; i < buttons.size(); i++) {
        if (buttons.get(i).getSelection()) {
          return i;
        }
      }
      return -1;
    }

    interface Listener {
      void selectionChanged();
    }
  }
}
