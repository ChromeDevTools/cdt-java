// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.LaunchParams.ValueConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Basic implementation of launch configuration tab. It holds input parameters and created
 * UI elements. The class is abstract and requires a set of fields to be described in
 * a ancestor. This way it can automatically handle main tab lifecycle by loading, storing
 * and initializing them.
 * @param <ELEMENTS> type interface holding UI elements
 * @param <PARAMS> type of interface holding input parameters
 */
abstract class TabBase<ELEMENTS, PARAMS> extends AbstractLaunchConfigurationTab {
  private final PARAMS params;
  private ELEMENTS tabElements = null;

  protected TabBase(PARAMS params) {
    this.params = params;
  }

  protected PARAMS getParams() {
    return params;
  }

  @Override
  public void createControl(Composite parent) {
    Runnable modifyListener = new Runnable() {
      @Override public void run() {
        updateLaunchConfigurationDialog();
      }
    };

    tabElements = createElements(parent, modifyListener);
  }

  protected abstract ELEMENTS createElements(Composite parent, Runnable modifyListener);

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    for (TabField<?, ?, ?, PARAMS> field : getTabFields()) {
      field.setDefault(configuration, getParams());
    }
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    for (TabField<?, ?, ? super ELEMENTS, ?> field : getTabFields()) {
      field.initializeFrom(tabElements, configuration);
    }
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    for (TabField<?, ?, ? super ELEMENTS, ?> field : getTabFields()) {
      field.saveToConfig(tabElements, configuration);
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
    if (messageData == null) {
      messageData = MessageData.EMPTY_OK;
    }

    if (messageData.isValid()) {
      setMessage(messageData.getMessage());
      setErrorMessage(null);
    } else {
      setMessage(null);
      setErrorMessage(messageData.getMessage());
    }
    return messageData.isValid();
  }

  /**
   * Tries to check whether config is valid and return message or fails with exception.
   */
  protected abstract MessageData isValidImpl(ILaunchConfiguration config) throws CoreException;

  /**
   * Describes a tab error/warning message.
   */
  protected static class MessageData {
    public static final MessageData EMPTY_OK = new MessageData(true, null);

    private final boolean valid;
    private final String message;

    public MessageData(boolean isValid, String message) {
      this.valid = isValid;
      this.message = message;
    }
    public boolean isValid() {
      return valid;
    }
    public String getMessage() {
      return message;
    }
  }

  protected abstract List<? extends TabField<?, ?, ? super ELEMENTS, PARAMS>> getTabFields();

  /**
   * A dialog window tab field description. It is a static description -- it has no reference to
   * a particular element instance.
   * @param <P> physical type of field as stored in config; used internally
   * @param <L> logical type of field used in runtime operations
   * @param <E> interface to dialog elements
   * @param <C> context that holds immutable input parameter of the tab dialog
   */
  static class TabField<P, L, E, C> {
    private final String configAttributeName;
    private final TypedMethods<P> typedMethods;
    private final FieldAccess<L, E> fieldAccess;
    private final DefaultsProvider<L, C> defaultsProvider;
    private final ValueConverter<P, L> valueConverter;

    TabField(String configAttributeName, TypedMethods<P> typedMethods,
        FieldAccess<L, E> fieldAccess, DefaultsProvider<L, C> defaultsProvider,
        ValueConverter<P, L> valueConverter) {
      this.typedMethods = typedMethods;
      this.defaultsProvider = defaultsProvider;
      this.configAttributeName = configAttributeName;
      this.fieldAccess = fieldAccess;
      this.valueConverter = valueConverter;
    }

    void saveToConfig(E tabElements, ILaunchConfigurationWorkingCopy config) {
      L logicalValue = fieldAccess.getValue(tabElements);
      P persistentValue = valueConverter.encode(logicalValue);
      typedMethods.setConfigAttribute(config, configAttributeName, persistentValue);
    }

    void initializeFrom(E tabElements, ILaunchConfiguration config) {
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

    public void setDefault(ILaunchConfigurationWorkingCopy config, C context) {
      L value = defaultsProvider.getInitialConfigValue(context);
      if (value != null) {
        P persistentValue = valueConverter.encode(value);
        typedMethods.setConfigAttribute(config, configAttributeName, persistentValue);
      }
    }
  }

  static abstract class FieldAccess<T, E> {
    abstract void setValue(T value, E tabElements);
    abstract T getValue(E tabElements);
  }

  static abstract class FieldEditorAccess<T, E> extends FieldAccess<T, E> {
    private final TypedMethods<T> fieldType;

    FieldEditorAccess(TypedMethods<T> fieldType) {
      this.fieldType = fieldType;
    }

    @Override
    void setValue(T value, E tabElements) {
      FieldEditor fieldEditor = getFieldEditor(tabElements);
      fieldType.setStoreDefaultValue(fieldEditor.getPreferenceStore(),
          fieldEditor.getPreferenceName(), value);
      fieldEditor.loadDefault();
    }

    @Override
    T getValue(E tabElements) {
      FieldEditor fieldEditor = getFieldEditor(tabElements);
      storeEditor(fieldEditor, getEditorErrorValue());
      return fieldType.getStoreValue(fieldEditor.getPreferenceStore(),
          fieldEditor.getPreferenceName());
    }

    abstract FieldEditor getFieldEditor(E tabElements);
    abstract String getEditorErrorValue();
  }

  static abstract class DefaultsProvider<T, C> {
    abstract T getFallbackValue();
    abstract T getInitialConfigValue(C context);
  }

  /**
   * Provides uniform access to various signatures of config and store methods.
   */
  static abstract class TypedMethods<T> {
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

  protected static Composite createDefaultComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);

    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    composite.setLayout(layout);

    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    composite.setLayoutData(data);

    return composite;
  }

  protected static Composite createInnerComposite(Composite parent, int numColumns) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(numColumns, false));
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    composite.setLayoutData(gd);
    return composite;
  }

  protected static GridLayout createHtmlStyleGridLayout(int numberOfColumns) {
    GridLayout layout = new GridLayout(numberOfColumns, false);
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    return layout;
  }

  private static void storeEditor(FieldEditor editor, String errorValue) {
    if (editor.isValid()) {
      editor.store();
    } else {
      editor.getPreferenceStore().setValue(editor.getPreferenceName(), errorValue);
    }
  }

  static class RadioButtonsLogic<K> {
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

  static void addRadioButtonSwitcher(final Collection<Button> buttons) {
    SelectionListener selectionListener = new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }
      public void widgetSelected(SelectionEvent e) {
        if (e.widget instanceof Button) {
          Button button = (Button) e.widget;
          if (!button.getSelection()) {
            return;
          }
          for (Button other : buttons) {
            if (other == button) {
              continue;
            }
            other.setSelection(false);
          }
        }
      }
    };

    for (Button button : buttons) {
      if ((button.getStyle() & SWT.NO_RADIO_GROUP) == 0) {
        throw new IllegalArgumentException();
      }
      button.addSelectionListener(selectionListener);
    }
  }
}
