// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.ChromiumSourceDirector;
import org.chromium.debug.core.SourceNameMapperContainer;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
import org.chromium.debug.core.model.LaunchParams;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.IPersistableSourceLocator2;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
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
  private static final String HOST_FIELD_NAME = "host_field"; //$NON-NLS-1$
  private static final String PORT_FIELD_NAME = "port_field"; //$NON-NLS-1$
  private static final String ADD_NETWORK_CONSOLE_FIELD_NAME =
      "add_network_console_field"; //$NON-NLS-1$

  private static final String INACCURATE_SOURCE_LOOKUP =
      "inaccurate_source_lookup"; //$NON-NLS-1$

   // However, recommended range is [1024, 32767].
  private static final int minimumPortValue = 0;
  private static final int maximumPortValue = 65535;

  private final SourceContainerChecker sourceContainerChecker = new SourceContainerChecker();
  private final HostChecker hostChecker;
  private TabElements tabElements = null;

  /**
   * Possibly checks host property in config.
   */
  public static abstract class HostChecker {
    public abstract String getWarning(ILaunchConfiguration config) throws CoreException;

    public static HostChecker FOR_CHROME = new HostChecker() {
      @Override
      public String getWarning(ILaunchConfiguration config) throws CoreException {
        String host = config.getAttribute(LaunchParams.CHROMIUM_DEBUG_HOST, ""); //$NON-NLS-1$
        if (!LOCAL_HOST_NAMES.contains(host.toLowerCase())) {
          return Messages.ChromiumRemoteTab_CONNECTION_FROM_LOCALHOST_WARNING;
        }
        return null;
      }
    };

    private static final Collection<String> LOCAL_HOST_NAMES =
        Arrays.asList("localhost", "127.0.0.1"); //$NON-NLS-1$ //$NON-NLS-2$
  }


  ChromiumRemoteTab(HostChecker hostChecker) {
    this.hostChecker = hostChecker;
  }

  public void createControl(Composite parent) {
    tabElements = createControlImpl(parent);
  }

  private TabElements createControlImpl(Composite parent) {
    Composite composite = createDefaultComposite(parent);

    IPropertyChangeListener modifyListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        updateLaunchConfigurationDialog();
      }
    };

    PreferenceStore store = new PreferenceStore();
    final StringFieldEditor debugHost;
    final IntegerFieldEditor debugPort;
    final BooleanFieldEditor addNetworkConsole;
    {
      Group connectionGroup = new Group(composite, 0);
      connectionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      connectionGroup.setText(Messages.ChromiumRemoteTab_CONNECTION_GROUP);
      connectionGroup.setLayout(new GridLayout(1, false));

      Composite propertiesComp = createInnerComposite(connectionGroup, 2);

      // Host text field
      debugHost = new StringFieldEditor(HOST_FIELD_NAME,
          Messages.ChromiumRemoteTab_HostLabel, propertiesComp);
      debugHost.setPropertyChangeListener(modifyListener);
      debugHost.setPreferenceStore(store);

      // Port text field
      debugPort = new IntegerFieldEditor(PORT_FIELD_NAME,
          Messages.ChromiumRemoteTab_PortLabel, propertiesComp);
      debugPort.setPropertyChangeListener(modifyListener);
      debugPort.setPreferenceStore(store);

      addNetworkConsole =
          new BooleanFieldEditor(ADD_NETWORK_CONSOLE_FIELD_NAME,
              Messages.ChromiumRemoteTab_ShowDebuggerNetworkCommunication, propertiesComp);
      addNetworkConsole.setPreferenceStore(store);
      addNetworkConsole.setPropertyChangeListener(modifyListener);
    }

    final RadioButtonsLogic breakpointRadioButtons;
    {
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
      breakpointRadioButtons =
          new RadioButtonsLogic(radioButtons, radioButtonsListener);
    }

    final BooleanFieldEditor inaccurateSourceLookup;
    {
      Composite hiddenComposite = createInnerComposite(composite, 2);

      // TODO: externalize user message (or better redesign control).
      inaccurateSourceLookup =
          new BooleanFieldEditor(INACCURATE_SOURCE_LOOKUP,
              "Inaccurate Source Lookup", hiddenComposite);
      inaccurateSourceLookup.setPreferenceStore(store);
      inaccurateSourceLookup.setPropertyChangeListener(modifyListener);
    }

    return new TabElements() {
      @Override public StringFieldEditor getHost() {
        return debugHost;
      }
      @Override public IntegerFieldEditor getPort() {
        return debugPort;
      }
      @Override public BooleanFieldEditor getAddNetworkConsole() {
        return addNetworkConsole;
      }
      @Override public RadioButtonsLogic getBreakpointRadioButtons() {
        return breakpointRadioButtons;
      }
      @Override public BooleanFieldEditor getInaccurateSourceLookup() {
        return inaccurateSourceLookup;
      }
    };
  }

  public String getName() {
    return Messages.ChromiumRemoteTab_RemoteTabName;
  }

  public void initializeFrom(ILaunchConfiguration config) {
    for (TabField<?> field : TAB_FIELDS) {
      field.initializeFrom(tabElements, config);
    }
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    for (TabField<?> field : TAB_FIELDS) {
      field.saveToConfig(tabElements, config);
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    MessageData messageData;
    try {
      messageData = isValidImpl(config);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(new Exception("Unexpected storage problem", e)); //$NON-NLS-1$
      messageData = new MessageData(true, "Internal error " + e.getMessage()); //$NON-NLS-1$
    }

    if (messageData.isValid) {
      setMessage(messageData.message);
      setErrorMessage(null);
    } else {
      setMessage(null);
      setErrorMessage(messageData.message);
    }
    return messageData.isValid;
  }

  /**
   * Tries to check whether config is valid and return message or fails with exception.
   */
  private MessageData isValidImpl(ILaunchConfiguration config) throws CoreException {
    int port = config.getAttribute(LaunchParams.CHROMIUM_DEBUG_PORT, -1);
    if (port < minimumPortValue || port > maximumPortValue) {
      return new MessageData(false, Messages.ChromiumRemoteTab_InvalidPortNumberError);
    }
    final String message = getWarning(config);

    return new MessageData(true, message);
  }

  /**
   * Checks config for warnings and returns first found or null.
   */
  private String getWarning(ILaunchConfiguration config) throws CoreException {
    if (hostChecker != null) {
      String hostWarning = hostChecker.getWarning(config);
      if (hostWarning != null) {
        return hostWarning;
      }
    }
    return sourceContainerChecker.check(config);
  }

  private static class MessageData {
    MessageData(boolean isValid, String message) {
      this.isValid = isValid;
      this.message = message;
    }
    final boolean isValid;
    final String message;
  }


  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    for (TabField<?> field : TAB_FIELDS) {
      field.setDefault(config);
    }
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

  private interface TabElements {
    StringFieldEditor getHost();
    BooleanFieldEditor getInaccurateSourceLookup();
    IntegerFieldEditor getPort();
    BooleanFieldEditor getAddNetworkConsole();
    RadioButtonsLogic getBreakpointRadioButtons();
  }

  /**
   * A dialog window tab field description. It is a static description -- it has no reference to
   * a particular element instance.
   * @param <T> type of field as stored in config; used internally
   */
  private static class TabField<T> {
    private final String configAttributeName;
    private final TypedMethods<T> typedMethods;
    private final FieldAccess<T> fieldAccess;
    private final DefaultsProvider<T> defaultsProvider;

    TabField(String configAttributeName, TypedMethods<T> typedMethods,
        FieldAccess<T> fieldAccess, DefaultsProvider<T> defaultsProvider) {
      this.typedMethods = typedMethods;
      this.defaultsProvider = defaultsProvider;
      this.configAttributeName = configAttributeName;
      this.fieldAccess = fieldAccess;
    }

    void saveToConfig(TabElements tabElements, ILaunchConfigurationWorkingCopy config) {
      T value = fieldAccess.getValue(tabElements);
      typedMethods.setConfigAttribute(config, configAttributeName, value);
    }

    void initializeFrom(TabElements tabElements, ILaunchConfiguration config) {
      T fallbackValue = defaultsProvider.getFallbackValue();
      T value;
      try {
        value = typedMethods.getConfigAttribute(config, configAttributeName, fallbackValue);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(new Exception("Unexpected storage problem", e)); //$NON-NLS-1$
        value = fallbackValue;
      }
      fieldAccess.setValue(value, tabElements);
    }

    public void setDefault(ILaunchConfigurationWorkingCopy config) {
      T value = defaultsProvider.getInitialConfigValue();
      if (value != null) {
        typedMethods.setConfigAttribute(config, configAttributeName, value);
      }
    }
  }

  private static abstract class FieldAccess<T> {
    abstract void setValue(T value, TabElements tabElements);
    abstract T getValue(TabElements tabElements);
  }

  private static abstract class FieldEditorAccess<T> extends FieldAccess<T> {
    private final TypedMethods<T> fieldType;

    FieldEditorAccess(TypedMethods<T> fieldType) {
      this.fieldType = fieldType;
    }

    @Override
    void setValue(T value, TabElements tabElements) {
      FieldEditor fieldEditor = getFieldEditor(tabElements);
      fieldType.setStoreDefaultValue(fieldEditor.getPreferenceStore(),
          fieldEditor.getPreferenceName(), value);
      fieldEditor.loadDefault();
    }

    @Override
    T getValue(TabElements tabElements) {
      FieldEditor fieldEditor = getFieldEditor(tabElements);
      storeEditor(fieldEditor, getEditorErrorValue());
      return fieldType.getStoreValue(fieldEditor.getPreferenceStore(),
          fieldEditor.getPreferenceName());
    }

    abstract FieldEditor getFieldEditor(TabElements tabElements);
    abstract String getEditorErrorValue();
  }

  private static abstract class DefaultsProvider<T> {
    abstract T getFallbackValue();
    abstract T getInitialConfigValue();
  }

  /**
   * Provides uniform access to various signatures of config and store methods.
   */
  private static abstract class TypedMethods<T> {
    abstract T getConfigAttribute(ILaunchConfiguration config, String attributeName,
        T defaultValue) throws CoreException;
    abstract void setConfigAttribute(ILaunchConfigurationWorkingCopy config, String attributeName,
        T value);

    abstract T getStoreValue(IPreferenceStore store, String preferenceName);
    abstract void setStoreDefaultValue(IPreferenceStore store, String propertyName, T value);

    static final TypedMethods<String> STRING = new TypedMethods<String>() {
      String getConfigAttribute(ILaunchConfiguration config, String attributeName,
          String defaultValue) throws CoreException {
        return config.getAttribute(attributeName, defaultValue);
      }
      public void setConfigAttribute(ILaunchConfigurationWorkingCopy config, String attributeName,
          String value) {
        config.setAttribute(attributeName, value);
      }
      void setStoreDefaultValue(IPreferenceStore store, String propertyName, String value) {
        store.setDefault(propertyName, value);
      }
      String getStoreValue(IPreferenceStore store, String preferenceName) {
        return store.getString(preferenceName);
      }
    };

    static final TypedMethods<Integer> INT = new TypedMethods<Integer>() {
      public void setConfigAttribute(ILaunchConfigurationWorkingCopy config, String attributeName,
          Integer value) {
        config.setAttribute(attributeName, value);
      }
      Integer getConfigAttribute(ILaunchConfiguration config, String attributeName,
          Integer defaultValue) throws CoreException {
        return config.getAttribute(attributeName, defaultValue);
      }
      void setStoreDefaultValue(IPreferenceStore store, String propertyName, Integer value) {
        store.setDefault(propertyName, value);
      }
      Integer getStoreValue(IPreferenceStore store, String preferenceName) {
        return store.getInt(preferenceName);
      }
    };

    static final TypedMethods<Boolean> BOOL = new TypedMethods<Boolean>() {
      public void setConfigAttribute(ILaunchConfigurationWorkingCopy config, String attributeName,
          Boolean value) {
        config.setAttribute(attributeName, value);
      }
      Boolean getConfigAttribute(ILaunchConfiguration config, String attributeName,
          Boolean defaultValue) throws CoreException {
        return config.getAttribute(attributeName, defaultValue);
      }
      void setStoreDefaultValue(IPreferenceStore store, String propertyName, Boolean value) {
        store.setDefault(propertyName, value);
      }
      Boolean getStoreValue(IPreferenceStore store, String preferenceName) {
        return store.getBoolean(preferenceName);
      }
    };
  }

  private static final List<TabField<?>> TAB_FIELDS;
  static {
    List<TabField<?>> list = new ArrayList<ChromiumRemoteTab.TabField<?>>(4);

    list.add(new TabField<String>(
        LaunchParams.CHROMIUM_DEBUG_HOST, TypedMethods.STRING,
        new FieldEditorAccess<String>(TypedMethods.STRING) {
          @Override
          FieldEditor getFieldEditor(TabElements tabElements) {
            return tabElements.getHost();
          }
          String getEditorErrorValue() {
            return ""; //$NON-NLS-1$
          }
        },
        new DefaultsProvider<String>() {
          @Override String getFallbackValue() {
            return PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_HOST);
          }
          @Override
          String getInitialConfigValue() {
            return getFallbackValue();
          }
        }));

    list.add(new TabField<Integer>(
        LaunchParams.CHROMIUM_DEBUG_PORT, TypedMethods.INT,
        new FieldEditorAccess<Integer>(TypedMethods.INT) {
          @Override
          FieldEditor getFieldEditor(TabElements tabElements) {
            return tabElements.getPort();
          }
          String getEditorErrorValue() {
            return "-1"; //$NON-NLS-1$
          }
        },
        new DefaultsProvider<Integer>() {
          @Override Integer getFallbackValue() {
            return PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT);
          }
          @Override
          Integer getInitialConfigValue() {
            return getFallbackValue();
          }
        }));

    list.add(new TabField<Boolean>(
        LaunchParams.ADD_NETWORK_CONSOLE, TypedMethods.BOOL,
        new FieldEditorAccess<Boolean>(TypedMethods.BOOL) {
          FieldEditor getFieldEditor(TabElements tabElements) {
            return tabElements.getAddNetworkConsole();
          }
          String getEditorErrorValue() {
            return ""; //$NON-NLS-1$
          }
        },
        new DefaultsProvider<Boolean>() {
          @Override Boolean getFallbackValue() {
            return false;
          }
          @Override
          Boolean getInitialConfigValue() {
            return null;
          }
        }));

    list.add(new TabField<String>(
        LaunchParams.BREAKPOINT_SYNC_DIRECTION, TypedMethods.STRING, new FieldAccess<String>() {
          @Override
          void setValue(String value, TabElements tabElements) {
            int breakpointOptionIndex = LaunchParams.findBreakpointOption(value);
            tabElements.getBreakpointRadioButtons().select(breakpointOptionIndex);
          }
          @Override
          String getValue(TabElements tabElements) {
            int breakpointOption = tabElements.getBreakpointRadioButtons().getSelected();
            return LaunchParams.BREAKPOINT_OPTIONS.get(breakpointOption).getDirectionStringValue();
          }
        },
        new DefaultsProvider<String>() {
          @Override String getFallbackValue() {
            return Direction.MERGE.toString();
          }
          @Override
          String getInitialConfigValue() {
            return null;
          }
        }));

    list.add(new TabField<Boolean>(
        LaunchParams.INACCURATE_SOURCE_LOOKUP, TypedMethods.BOOL,
        new FieldEditorAccess<Boolean>(TypedMethods.BOOL) {
          FieldEditor getFieldEditor(TabElements tabElements) {
            return tabElements.getInaccurateSourceLookup();
          }
          String getEditorErrorValue() {
            return ""; //$NON-NLS-1$
          }
        },
        new DefaultsProvider<Boolean>() {
          @Override Boolean getFallbackValue() {
            return false;
          }
          @Override
          Boolean getInitialConfigValue() {
            return null;
          }
        }));

    TAB_FIELDS = Collections.unmodifiableList(list);
  }

  private static class SourceContainerChecker {
    public String check(ILaunchConfiguration config) {
      if (!ChromiumSourceDirector.isInaccurateMode(config)) {
        return null;
      }

      ISourceLookupDirector director;
      try {
        director = read(config);
      } catch (CoreException e) {
        return null;
      }
      if (director == null) {
        return null;
      }
      for (ISourceContainer sourceContainer : director.getSourceContainers()) {
        if (sourceContainer instanceof SourceNameMapperContainer) {
          return Messages.ChromiumRemoteTab_INACCURATE_CONTAINER_WARNING;
        }
      }
      return null;
    }

    private ISourceLookupDirector read(ILaunchConfiguration config) throws CoreException {
      String memento = config.getAttribute(
          ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
      if (memento == null) {
        return null;
      }
      String type = config.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String)null);
      if (type == null) {
        type = config.getType().getSourceLocatorId();
      }
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ISourceLocator locator = launchManager.newSourceLocator(type);
      if (locator instanceof IPersistableSourceLocator2 == false) {
        return null;
      }
      ISourceLookupDirector director = (ISourceLookupDirector) locator;
      director.initializeFromMemento(memento, config);
      return director;
    }
  }
}
