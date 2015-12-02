// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
import org.chromium.debug.core.model.LaunchParams.LookupMode;
import org.chromium.debug.core.model.LaunchParams.ValueConverter;
import org.chromium.debug.ui.launcher.LaunchTabGroup.Params;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.IPersistableSourceLocator2;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.ui.SWTFactory;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * The "Remote" tab for the Chromium JavaScript launch tab group.
 * @param <ELEMENTS> type used for access to created dialog elements; it is used internally
 *     and allows to subclass {@link ChromiumRemoteTab} with additional dialog controls
 */
public abstract class ChromiumRemoteTab<ELEMENTS> extends TabBase<ELEMENTS, Params> {
  private static final String HOST_FIELD_NAME = "host_field"; //$NON-NLS-1$
  private static final String PORT_FIELD_NAME = "port_field"; //$NON-NLS-1$
  private static final String ADD_NETWORK_CONSOLE_FIELD_NAME =
      "add_network_console_field"; //$NON-NLS-1$

   // However, recommended range is [1024, 32767].
  private static final int minimumPortValue = 0;
  private static final int maximumPortValue = 65535;

  private final SourceContainerChecker sourceContainerChecker = new SourceContainerChecker();

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
    super(params);
  }

  @Override
  protected ELEMENTS createElements(Composite parent, Runnable modifyListener) {
    Composite composite = createDefaultComposite(parent);
    setControl(composite);

    PreferenceStore store = new PreferenceStore();

    composite.setFont(parent.getFont());

    return createDialogElements(composite, modifyListener, store);
  }

  protected abstract ELEMENTS createDialogElements(Composite composite,
      Runnable modifyListener, PreferenceStore store);

  protected static TabElements createBasicTabElements(Composite composite,
      final Runnable modifyListener, PreferenceStore store, Params params) {
    final StringFieldEditor debugHost;
    final IntegerFieldEditor debugPort;
    final BooleanFieldEditor addNetworkConsole;
    {
      Group connectionGroup = new Group(composite, 0);
      connectionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      connectionGroup.setText(Messages.ChromiumRemoteTab_CONNECTION_GROUP);
      connectionGroup.setLayout(new GridLayout(1, false));


      IPropertyChangeListener propertyModifyListener = new IPropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
          modifyListener.run();
        }
      };


      Composite propertiesComp = createInnerComposite(connectionGroup, 2);

      // Host text field
      debugHost = new StringFieldEditor(HOST_FIELD_NAME,
          Messages.ChromiumRemoteTab_HostLabel, propertiesComp);
      debugHost.setPropertyChangeListener(propertyModifyListener);
      debugHost.setPreferenceStore(store);

      // Port text field
      debugPort = new IntegerFieldEditor(PORT_FIELD_NAME,
          Messages.ChromiumRemoteTab_PortLabel, propertiesComp);
      debugPort.setPropertyChangeListener(propertyModifyListener);
      debugPort.setPreferenceStore(store);

      addNetworkConsole =
          new BooleanFieldEditor(ADD_NETWORK_CONSOLE_FIELD_NAME,
              Messages.ChromiumRemoteTab_ShowDebuggerNetworkCommunication, propertiesComp);
      addNetworkConsole.setPreferenceStore(store);
      addNetworkConsole.setPropertyChangeListener(propertyModifyListener);
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
    };
  }

  public String getName() {
    return Messages.ChromiumRemoteTab_RemoteTabName;
  }

  @Override
  protected MessageData isValidImpl(ILaunchConfiguration config) throws CoreException {
    int port = config.getAttribute(LaunchParams.CHROMIUM_DEBUG_PORT, -1);
    if (port < minimumPortValue || port > maximumPortValue) {
      return new MessageData(false, Messages.ChromiumRemoteTab_InvalidPortNumberError);
    }
    String message = getWarning(config);
    return new MessageData(true, message);
  }

  /**
   * Checks config for warnings and returns first found or null.
   */
  protected String getWarning(ILaunchConfiguration config) throws CoreException {
    HostChecker hostChecker = getParams().getHostChecker();
    if (hostChecker != null) {
      String hostWarning = hostChecker.getWarning(config);
      if (hostWarning != null) {
        return hostWarning;
      }
    }
    return sourceContainerChecker.check(config);
  }


  @Override
  public Image getImage() {
    return DebugUITools.getImage(IDebugUIConstants.IMG_LCL_DISCONNECT);
  }

  interface TabElements {
    StringFieldEditor getHost();
    IntegerFieldEditor getPort();
    BooleanFieldEditor getAddNetworkConsole();
  }

  static final TabFieldList<TabElements, Params> BASIC_TAB_FIELDS;
  static {
    List<TabField<?, ?, ? super TabElements, Params>> list =
        new ArrayList<ChromiumRemoteTab.TabField<?, ?, ? super TabElements, Params>>(4);

    list.add(new TabField<String, String, TabElements, Params>(
        LaunchParams.CHROMIUM_DEBUG_HOST, TypedMethods.STRING,
        new FieldEditorAccess<String, TabElements>(TypedMethods.STRING) {
          @Override
          FieldEditor getFieldEditor(TabElements tabElements) {
            return tabElements.getHost();
          }
          String getEditorErrorValue() {
            return ""; //$NON-NLS-1$
          }
        },
        new DefaultsProvider<String, Params>() {
          @Override String getFallbackValue() {
            return PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_HOST);
          }
          @Override String getInitialConfigValue(Params context) {
            return getFallbackValue();
          }
        },
        ValueConverter.<String>getTrivial()));

    list.add(new TabField<Integer, Integer, TabElements, Params>(
        LaunchParams.CHROMIUM_DEBUG_PORT, TypedMethods.INT,
        new FieldEditorAccess<Integer, TabElements>(TypedMethods.INT) {
          @Override
          FieldEditor getFieldEditor(TabElements tabElements) {
            return tabElements.getPort();
          }
          String getEditorErrorValue() {
            return "-1"; //$NON-NLS-1$
          }
        },
        new DefaultsProvider<Integer, Params>() {
          @Override Integer getFallbackValue() {
            return PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT);
          }
          @Override Integer getInitialConfigValue(Params context) {
            return getFallbackValue();
          }
        },
        ValueConverter.<Integer>getTrivial()));

    list.add(new TabField<Boolean, Boolean, TabElements, Params>(
        LaunchParams.ADD_NETWORK_CONSOLE, TypedMethods.BOOL,
        new FieldEditorAccess<Boolean, TabElements>(TypedMethods.BOOL) {
          FieldEditor getFieldEditor(TabElements tabElements) {
            return tabElements.getAddNetworkConsole();
          }
          String getEditorErrorValue() {
            return ""; //$NON-NLS-1$
          }
        },
        new DefaultsProvider<Boolean, Params>() {
          @Override Boolean getFallbackValue() {
            return false;
          }
          @Override Boolean getInitialConfigValue(Params context) {
            return null;
          }
        },
        ValueConverter.<Boolean>getTrivial()));

    BASIC_TAB_FIELDS = createFieldListImpl(list);
  }

  private static class SourceContainerChecker {
    public String check(ILaunchConfiguration config) {
      LookupMode lookupMode;
      try {
        lookupMode = ChromiumSourceDirector.readLookupMode(config);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(e);
        return null;
      }
      if (lookupMode != LookupMode.AUTO_DETECT) {
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
          return Messages.ChromiumRemoteTab_AUTO_DETECT_CONTAINER_WARNING;
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

  static abstract class DevToolsProtocolBase extends ChromiumRemoteTab<DevToolsElements> {
    DevToolsProtocolBase(Params params) {
      super(params);
    }

    @Override
    protected DevToolsElements createDialogElements(Composite composite,
        final Runnable modifyListener, PreferenceStore store) {
      final TabElements basicElements =
          createBasicTabElements(composite, modifyListener, store, getParams());

      RadioButtonsLogic.Listener radioButtonsListener =
          new RadioButtonsLogic.Listener() {
            public void selectionChanged() {
              modifyListener.run();
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
          button.setFont(composite.getFont());
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

      return new DevToolsElements() {
        @Override
        public RadioButtonsLogic<Integer> getBreakpointRadioButtons() {
          return breakpointRadioButtons;
        }

        @Override
        public TabElements getBase() {
          return basicElements;
        }
      };
    }

    @Override
    protected TabFieldList<? super DevToolsElements, ? super Params> getTabFields() {
      return TAB_FIELD_LIST;
    }

    private static final TabFieldList<? super DevToolsElements, ? super Params> TAB_FIELD_LIST;
    static {
      TabField<String, String, DevToolsElements, Params> breakpointSyncField =
          new TabField<String, String, DevToolsElements, Params>(
          LaunchParams.BREAKPOINT_SYNC_DIRECTION, TypedMethods.STRING,
              new FieldAccess<String, DevToolsElements>() {
            @Override
            void setValue(String value, DevToolsElements tabElements) {
              int breakpointOptionIndex = LaunchParams.findBreakpointOption(value);
              tabElements.getBreakpointRadioButtons().select(breakpointOptionIndex);
            }
            @Override
            String getValue(DevToolsElements tabElements) {
              int breakpointOption = tabElements.getBreakpointRadioButtons().getSelected();
              return LaunchParams.BREAKPOINT_OPTIONS.get(breakpointOption)
                  .getDirectionStringValue();
            }
          },
          new DefaultsProvider<String, Params>() {
            @Override String getFallbackValue() {
              return Direction.MERGE.toString();
            }
            @Override String getInitialConfigValue(Params context) {
              return null;
            }
          },
          ValueConverter.<String>getTrivial());

      List<TabFieldList<? super DevToolsElements, ? super Params>> subLists =
          new ArrayList<TabBase.TabFieldList<? super DevToolsElements,? super Params>>(2);
      subLists.add(createFieldListAdapting(BASIC_TAB_FIELDS,
          new Adapter<DevToolsElements, TabElements>() {
            @Override
            public TabElements get(DevToolsElements from) {
              return from.getBase();
            }
          }));
      subLists.add(createFieldListImpl(Collections.singletonList(breakpointSyncField)));

      TAB_FIELD_LIST = createCompositeFieldList(subLists);
    }
  }

  interface DevToolsElements {
    RadioButtonsLogic<Integer> getBreakpointRadioButtons();
    TabElements getBase();
  }

  static class Standalone extends DevToolsProtocolBase {
    public Standalone() {
      super(PARAMS);
    }

    private static final Params PARAMS = new Params(null,
        Messages.ChromiumRemoteTab_FILE_PATH, true);
  }
}
