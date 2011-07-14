// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.ChromiumSourceDirector;
import org.chromium.debug.core.SourceNameMapperContainer;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.LaunchParams.LookupAccurateness;
import org.chromium.debug.core.model.LaunchParams.ValueConverter;
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
import org.eclipse.swt.widgets.Label;

/**
 * The "Remote" tab for the Chromium JavaScript launch tab group.
 */
public class ChromiumRemoteTab extends AbstractLaunchConfigurationTab {
  private static final String HOST_FIELD_NAME = "host_field"; //$NON-NLS-1$
  private static final String PORT_FIELD_NAME = "port_field"; //$NON-NLS-1$
  private static final String ADD_NETWORK_CONSOLE_FIELD_NAME =
      "add_network_console_field"; //$NON-NLS-1$

   // However, recommended range is [1024, 32767].
  private static final int minimumPortValue = 0;
  private static final int maximumPortValue = 65535;

  private final SourceContainerChecker sourceContainerChecker = new SourceContainerChecker();
  private final Params params;
  private TabElements tabElements = null;

  public static class Params {
    private final HostChecker hostChecker;
    private final LookupAccurateness newConfigLookupAccurateness;
    private final String scriptNameDescription;

    public Params(HostChecker hostChecker,
        LookupAccurateness newConfigLookupAccurateness,
        String scriptNameDescription) {
      this.hostChecker = hostChecker;
      this.newConfigLookupAccurateness = newConfigLookupAccurateness;
      this.scriptNameDescription = scriptNameDescription;
    }

    HostChecker getHostChecker() {
      return hostChecker;
    }

    LookupAccurateness getNewConfigLookupAccurateness() {
      return newConfigLookupAccurateness;
    }

    String getScriptNameDescription() {
      return scriptNameDescription;
    }

    public static final Params CHROME = new Params(HostChecker.LOCAL_ONLY,
        LaunchParams.LookupAccurateness.INACCURATE, "URL");

    public static final Params STANDALONE = new Params(null,
        LaunchParams.LookupAccurateness.INACCURATE, "plain file path");

    public static final Params WIP = new Params(HostChecker.LOCAL_ONLY,
        LaunchParams.LookupAccurateness.ACCURATE, "URL");
  }

  /**
   * Possibly checks host property in config.
   */
  public static abstract class HostChecker {
    public abstract String getWarning(ILaunchConfiguration config) throws CoreException;

    public static HostChecker LOCAL_ONLY = new HostChecker() {
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


  ChromiumRemoteTab(Params params) {
    this.params = params;
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

    RadioButtonsLogic.Listener radioButtonsListener =
        new RadioButtonsLogic.Listener() {
          public void selectionChanged() {
            updateLaunchConfigurationDialog();
          }
        };

    final RadioButtonsLogic<Integer> breakpointRadioButtons;
    {
      Group breakpointGroup = new Group(composite, 0);
      breakpointGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      breakpointGroup.setText(Messages.ChromiumRemoteTab_BREAKPOINT_GROUP);
      breakpointGroup.setLayout(new GridLayout(1, false));

      Map<Integer, Button> buttonMap = new LinkedHashMap<Integer, Button>(3);
      for (LaunchParams.BreakpointOption option : LaunchParams.BREAKPOINT_OPTIONS) {
        Button button = new Button(breakpointGroup, SWT.RADIO);
        button.setFont(parent.getFont());
        button.setText(option.getLabel());
        GridData gd = new GridData();
        button.setLayoutData(gd);
        SWTFactory.setButtonDimensionHint(button);
        int index = buttonMap.size();
        buttonMap.put(index, button);
      }

      breakpointRadioButtons =
          new RadioButtonsLogic<Integer>(buttonMap, radioButtonsListener);
    }

    InaccurateOptionControl inaccurateOptionControl =
        new InaccurateOptionControl(composite, params.getScriptNameDescription());

    final RadioButtonsLogic<LookupAccurateness> inaccuratenessLogic =
        inaccurateOptionControl.createLogic(radioButtonsListener);

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
      @Override public RadioButtonsLogic<Integer> getBreakpointRadioButtons() {
        return breakpointRadioButtons;
      }
      @Override public RadioButtonsLogic<LookupAccurateness> getAccuratenessControl() {
        return inaccuratenessLogic;
      }
    };
  }

  public String getName() {
    return Messages.ChromiumRemoteTab_RemoteTabName;
  }

  public void initializeFrom(ILaunchConfiguration config) {
    for (TabField<?, ?> field : TAB_FIELDS) {
      field.initializeFrom(tabElements, config);
    }
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    for (TabField<?, ?> field : TAB_FIELDS) {
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
    HostChecker hostChecker = params.getHostChecker();
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
    for (TabField<?, ?> field : TAB_FIELDS) {
      field.setDefault(config, this);
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

  private static class RadioButtonsLogic<K> {
    private final Map<K, Button> buttons;
    RadioButtonsLogic(Map<K, Button> buttons, final Listener listener) {
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

      for (Button button : buttons.values()) {
        button.addSelectionListener(selectionListener);
      }
    }
    void select(K key) {
      for (Map.Entry<K, Button> en : buttons.entrySet()) {
        en.getValue().setSelection(en.getKey().equals(key));
      }
    }
    K getSelected() {
      for (Map.Entry<K, Button> en : buttons.entrySet()) {
        if (en.getValue().getSelection()) {
          return en.getKey();

        }
      }
      return null;
    }

    interface Listener {
      void selectionChanged();
    }
  }

  private static void addRadioButtonSwitcher(final Collection<Button> buttons) {
    SelectionListener selectionListener = new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }
      public void widgetSelected(SelectionEvent e) {
        if (e.widget instanceof Button) {
          Button button = (Button) e.widget;
          if (button.getSelection()) {
            for (Button other : buttons) {
              if (other != button) {
                other.setSelection(false);
              }
            }
          }
        }
      }
    };

    for (Button button : buttons) {
      if ( (button.getStyle() & SWT.NO_RADIO_GROUP) == 0) {
        throw new IllegalArgumentException();
      }
      button.addSelectionListener(selectionListener);
    }
  }

  private interface TabElements {
    StringFieldEditor getHost();
    IntegerFieldEditor getPort();
    BooleanFieldEditor getAddNetworkConsole();
    RadioButtonsLogic<Integer> getBreakpointRadioButtons();
    RadioButtonsLogic<LookupAccurateness> getAccuratenessControl();
  }

  /**
   * A dialog window tab field description. It is a static description -- it has no reference to
   * a particular element instance.
   * @param <P> physical type of field as stored in config; used internally
   * @param <L> logical type of field used in runtime operations
   */
  private static class TabField<P, L> {
    private final String configAttributeName;
    private final TypedMethods<P> typedMethods;
    private final FieldAccess<L> fieldAccess;
    private final DefaultsProvider<L> defaultsProvider;
    private final ValueConverter<P, L> valueConverter;

    TabField(String configAttributeName, TypedMethods<P> typedMethods,
        FieldAccess<L> fieldAccess, DefaultsProvider<L> defaultsProvider,
        ValueConverter<P, L> valueConverter) {
      this.typedMethods = typedMethods;
      this.defaultsProvider = defaultsProvider;
      this.configAttributeName = configAttributeName;
      this.fieldAccess = fieldAccess;
      this.valueConverter = valueConverter;
    }

    void saveToConfig(TabElements tabElements, ILaunchConfigurationWorkingCopy config) {
      L logicalValue = fieldAccess.getValue(tabElements);
      P persistentValue = valueConverter.encode(logicalValue);
      typedMethods.setConfigAttribute(config, configAttributeName, persistentValue);
    }

    void initializeFrom(TabElements tabElements, ILaunchConfiguration config) {
      L fallbackLogicalValue = defaultsProvider.getFallbackValue();
      P fallbackPersistenValue = valueConverter.encode(fallbackLogicalValue);
      L value;
      try {
        P persistentValue = typedMethods.getConfigAttribute(config, configAttributeName,
            fallbackPersistenValue);
        value = valueConverter.decode(persistentValue);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(new Exception("Unexpected storage problem", e)); //$NON-NLS-1$
        value = fallbackLogicalValue;
      }
      fieldAccess.setValue(value, tabElements);
    }

    public void setDefault(ILaunchConfigurationWorkingCopy config, ChromiumRemoteTab tab) {
      L value = defaultsProvider.getInitialConfigValue(tab);
      if (value != null) {
        P persistentValue = valueConverter.encode(value);
        typedMethods.setConfigAttribute(config, configAttributeName, persistentValue);
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
    abstract T getInitialConfigValue(ChromiumRemoteTab tab);
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

  private static final List<TabField<?, ?>> TAB_FIELDS;
  static {
    List<TabField<?, ?>> list = new ArrayList<ChromiumRemoteTab.TabField<?, ?>>(4);

    list.add(new TabField<String, String>(
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
          @Override String getInitialConfigValue(ChromiumRemoteTab dialog) {
            return getFallbackValue();
          }
        },
        ValueConverter.<String>getTrivial()));

    list.add(new TabField<Integer, Integer>(
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
          @Override Integer getInitialConfigValue(ChromiumRemoteTab dialog) {
            return getFallbackValue();
          }
        },
        ValueConverter.<Integer>getTrivial()));

    list.add(new TabField<Boolean, Boolean>(
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
          @Override Boolean getInitialConfigValue(ChromiumRemoteTab dialog) {
            return null;
          }
        },
        ValueConverter.<Boolean>getTrivial()));

    list.add(new TabField<String, String>(
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
          @Override String getInitialConfigValue(ChromiumRemoteTab dialog) {
            return null;
          }
        },
        ValueConverter.<String>getTrivial()));

    list.add(new TabField<String, LookupAccurateness>(
        LaunchParams.SOURCE_LOOKUP_ACCURATENESS, TypedMethods.STRING,
        new FieldAccess<LookupAccurateness>() {
          @Override
          void setValue(LookupAccurateness value, TabElements tabElements) {
            tabElements.getAccuratenessControl().select(value);
          }
          @Override
          LookupAccurateness getValue(TabElements tabElements) {
            return tabElements.getAccuratenessControl().getSelected();
          }
        },
        new DefaultsProvider<LookupAccurateness>() {
          @Override LookupAccurateness getFallbackValue() {
            // TODO: support default value from eclipse variables.
            return LookupAccurateness.DEFAULT_VALUE;
          }
          @Override LookupAccurateness getInitialConfigValue(ChromiumRemoteTab dialog) {
            return dialog.params.getNewConfigLookupAccurateness();
          }
        },
        LookupAccurateness.STRING_CONVERTER));

    TAB_FIELDS = Collections.unmodifiableList(list);
  }

  private static class SourceContainerChecker {
    public String check(ILaunchConfiguration config) {
      LookupAccurateness accurateness;
      try {
        accurateness = ChromiumSourceDirector.readLookupAccurateness(config);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(e);
        return null;
      }
      if (accurateness != LookupAccurateness.INACCURATE) {
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

  private static GridLayout createHtmlStyleGridLayout(int numberOfColumns) {
    GridLayout layout = new GridLayout(numberOfColumns, false);
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    return layout;
  }

  /**
   * Dialog UI group of 2 radio buttons for lookup accurateness mode.
   */
  private static class InaccurateOptionControl {
    private final Map<LookupAccurateness, Button> buttons;
    InaccurateOptionControl(Composite container, String scriptNameFormatDescription) {
      buttons = new LinkedHashMap<LookupAccurateness, Button>();
      Group breakpointGroup = new Group(container, 0);
      breakpointGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      breakpointGroup.setText("Source look-up method for workspace files");
      breakpointGroup.setLayout(new GridLayout(1, false));

      buttons.put(LookupAccurateness.ACCURATE, createButtonBlock(breakpointGroup,
          "Accurate", "Full script names are used.",
          "JavaScript Source Name Mapper must be set up in Source tab."));
      buttons.put(LookupAccurateness.INACCURATE, createButtonBlock(breakpointGroup,
          "Inaccurate", "Only short file names are used.",
          "Actual script names are treated as: " + scriptNameFormatDescription));

      addRadioButtonSwitcher(buttons.values());
    }

    RadioButtonsLogic<LookupAccurateness> createLogic(RadioButtonsLogic.Listener listener) {
      return new RadioButtonsLogic<LookupAccurateness>(buttons, listener);
    }

    private static Button createButtonBlock(Composite parent, String buttonLabel,
        String descriptionLine1, String descriptionLine2) {
      Composite buttonComposite = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = createHtmlStyleGridLayout(2);
      buttonComposite.setLayout(gridLayout);
      Button button = new Button(buttonComposite, SWT.RADIO | SWT.NO_RADIO_GROUP);
      button.setText(buttonLabel + "   ");
      Label descriptionLine1Label = new Label(buttonComposite, SWT.NONE);
      descriptionLine1Label.setText(descriptionLine1);
      // Extra label to fill a grid in layout.
      new Label(buttonComposite, SWT.NONE);
      Label descriptionLine2Label = new Label(buttonComposite, SWT.NONE);
      descriptionLine2Label.setText(descriptionLine2);
      return button;
    }
  }
}
