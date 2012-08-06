// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.LaunchParams.ValueConverter;
import org.chromium.debug.ui.PluginUtil;
import org.chromium.debug.ui.launcher.LaunchTabGroup.Params;
import org.chromium.sdk.wip.WipBackend;
import org.chromium.sdk.wip.eclipse.BackendRegistry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

class WipRemoteTab extends ChromiumRemoteTab<WipRemoteTab.WipTabElements> {
  private final Map<String, WipBackend> backendMap;

  WipRemoteTab() {
    super(PARAMS);

    backendMap = new LinkedHashMap<String, WipBackend>();
    for (WipBackend b : BackendRegistry.INSTANCE.getBackends()) {
      backendMap.put(b.getId(), b);
    }
  }

  interface WipTabElements {
    BackendSelectorControl getBackendSelector();
    ChromiumRemoteTab.TabElements getBase();
  }

  @Override
  protected String getWarning(ILaunchConfiguration config) throws CoreException {
    String result = super.getWarning(config);
    if (result != null) {
      return result;
    }
    String backendId = config.getAttribute(LaunchParams.WIP_BACKEND_ID, (String) null);
    if (backendId == null) {
      return "Wip backend should be selected";
    }
    if (backendMap.get(backendId) == null) {
      return "Unknown Wip backend id";
    }
    return null;
  }

  @Override
  protected WipTabElements createDialogElements(Composite composite, final Runnable modifyListener,
      PreferenceStore store) {
    final TabElements basicElements =
        createBasicTabElements(composite, modifyListener, store, getParams());

    final BackendSelectorControl backendSelector =
        new BackendSelectorControl(composite, backendMap, modifyListener);
    backendSelector.getMainControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    return new WipTabElements() {
      @Override public BackendSelectorControl getBackendSelector() {
        return backendSelector;
      }
      @Override
      public TabElements getBase() {
        return basicElements;
      }
    };
  }

  @Override
  protected TabFieldList<? super WipTabElements, ? super Params> getTabFields() {
    return WIP_TAB_FIELDS;
  }

  private static final TabFieldList<? super WipTabElements, ? super Params>
      WIP_TAB_FIELDS;
  static {
    TabField<String, String, WipTabElements, Params> backendChooser =
        new TabField<String, String, WipTabElements, Params>(
        LaunchParams.WIP_BACKEND_ID, TypedMethods.STRING,
        new FieldAccess<String, WipTabElements>() {
          @Override void setValue(String value, WipTabElements tabElements) {
            tabElements.getBackendSelector().setId(value);
          }
          @Override String getValue(WipTabElements tabElements) {
            return tabElements.getBackendSelector().getId();
          }
        },
        new DefaultsProvider<String, Params>() {
          @Override String getFallbackValue() {
            // TODO: support default value from eclipse variables.
            return null;
          }
          @Override String getInitialConfigValue(Params context) {
            // TODO: support default value from eclipse variables.
            return null;
          }
        },
        ValueConverter.<String>getTrivial());

    List<TabFieldList<? super WipTabElements, ? super Params>> list  =
        new ArrayList<TabFieldList<? super WipTabElements, ? super Params>>(2);
    list.add(createFieldListAdapting(BASIC_TAB_FIELDS,
        new Adapter<WipTabElements, TabElements>() {
          @Override
          public TabElements get(WipTabElements from) {
            return from.getBase();
          }
       }));
    list.add(createFieldListImpl(Collections.singletonList(backendChooser)));

    WIP_TAB_FIELDS = createCompositeFieldList(list);
  }

  private static final Params PARAMS = new Params(HostChecker.LOCAL_ONLY,
      Messages.ChromiumRemoteTab_URL, false);

  /**
   * UI control elements that allows to choose {@link WipBackend}. It consists of a dialog group,
   * that contains a drop-down list with backend ids and a multiline text field with backend
   * description.
   */
  private static class BackendSelectorControl {
    private final Group mainControl;
    private final Combo combo;
    private final Text text;
    private final List<WipBackend> elements;
    private final String[] labelArray;

    BackendSelectorControl(Composite composite, Map<String, WipBackend> backendMap,
        final Runnable modifyListener) {
      Group backendGroup = new Group(composite, 0);
      // TODO: externalize it.
      backendGroup.setText("Wip backend");
      backendGroup.setLayout(new GridLayout(1, false));

      elements = new ArrayList<WipBackend>();
      elements.add(null);
      elements.addAll(backendMap.values());

      labelArray = new String[elements.size()];
      // TODO: externalize it.
      labelArray[0] = "Select backend";
      for (int i = 1; i < labelArray.length; i++) {
        labelArray[i] = elements.get(i).getId();
      }

      combo = new Combo(backendGroup, SWT.READ_ONLY);
      combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      combo.setFont(composite.getFont());
      combo.setItems(labelArray);
      combo.select(0);

      {
        text = new Text(backendGroup, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
        Font font = composite.getFont();
        text.setFont(font);
        GridData textLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        int fontHeight = PluginUtil.getFontMetrics(text, null).getHeight();

        textLayoutData.minimumHeight = fontHeight * 3;
        text.setLayoutData(textLayoutData);
      }

      combo.addSelectionListener(new SelectionListener() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          modifyListener.run();
          updateTextField();
        }

        @Override public void widgetDefaultSelected(SelectionEvent event) {
        }
      });

      mainControl = backendGroup;
    }

    public String getId() {
      WipBackend backend = elements.get(combo.getSelectionIndex());
      if (backend == null) {
        return null;
      } else {
        return backend.getId();
      }
    }

    public void setId(String id) {
      int index = Arrays.asList(labelArray).indexOf(id);
      if (index == -1) {
        index = 0;
      }
      combo.select(index);
      updateTextField();
    }

    private void updateTextField() {
      WipBackend backend = elements.get(combo.getSelectionIndex());
      String textContent;
      if (backend == null) {
        textContent = "";
      } else {
        textContent = backend.getDescription();
      }
      text.setText(textContent);
    }

    Group getMainControl() {
      return mainControl;
    }
  }
}
