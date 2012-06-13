// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.LaunchParams.LookupMode;
import org.chromium.debug.ui.launcher.LaunchTabGroup.Params;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * A launch configuration tab that holds source look-up and mapping parameters.
 */
public class ScriptMappingTab extends TabBase<ScriptMappingTab.Elements, Params>  {
  interface Elements {
    RadioButtonsLogic<LookupMode> getLookupMode();
  }

  ScriptMappingTab(Params params) {
    super(params);
  }

  @Override
  public Elements createElements(Composite parent, final Runnable modifyListener) {
    Composite composite = createDefaultComposite(parent);
    setControl(composite);

    LookupModeControl lookupModeControl =
        new LookupModeControl(composite, getParams().getScriptNameDescription());


    RadioButtonsLogic.Listener radioButtonsListener =
        new RadioButtonsLogic.Listener() {
          public void selectionChanged() {
            modifyListener.run();
          }
        };

    final RadioButtonsLogic<LookupMode> lookupModeLogic =
        lookupModeControl.createLogic(radioButtonsListener);

    return new Elements() {
      @Override public RadioButtonsLogic<LookupMode> getLookupMode() {
        return lookupModeLogic;
      }
    };
  }

  /**
   * Dialog UI group of 2 radio buttons for lookup mode.
   */
  private static class LookupModeControl {
    private final Map<LookupMode, Button> buttons;
    LookupModeControl(Composite container, String scriptNameFormatDescription) {
      buttons = new LinkedHashMap<LookupMode, Button>();
      Group group = new Group(container, 0);
      group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      group.setText(Messages.ChromiumRemoteTab_LOOKUP_GROUP_TITLE);
      group.setLayout(new GridLayout(1, false));

      buttons.put(LookupMode.EXACT_MATCH, createButtonBlock(group,
          Messages.ChromiumRemoteTab_EXACT_MATCH, Messages.ChromiumRemoteTab_EXACT_MATCH_LINE1,
          Messages.ChromiumRemoteTab_EXACT_MATCH_LINE2));

      buttons.put(LookupMode.AUTO_DETECT, createButtonBlock(group,
          Messages.ChromiumRemoteTab_AUTODETECT, Messages.ChromiumRemoteTab_AUTODETECT_LINE1,
          Messages.ChromiumRemoteTab_AUTODETECT_LINE2 + scriptNameFormatDescription));

      addRadioButtonSwitcher(buttons.values());
    }

    RadioButtonsLogic<LookupMode> createLogic(RadioButtonsLogic.Listener listener) {
      return new RadioButtonsLogic<LookupMode>(buttons, listener);
    }

    private static Button createButtonBlock(Composite parent, String buttonLabel,
        String descriptionLine1, String descriptionLine2) {
      Composite buttonComposite = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = createHtmlStyleGridLayout(3);
      buttonComposite.setLayout(gridLayout);
      Button button = new Button(buttonComposite, SWT.RADIO | SWT.NO_RADIO_GROUP);
      button.setText(buttonLabel);
      Label padding = new Label(buttonComposite, SWT.NONE);
      padding.setText("   "); //$NON-NLS-1$
      Label descriptionLine1Label = new Label(buttonComposite, SWT.NONE);
      descriptionLine1Label.setText(descriptionLine1);
      // Extra label to fill a grid in layout.
      new Label(buttonComposite, SWT.NONE);
      new Label(buttonComposite, SWT.NONE);
      Label descriptionLine2Label = new Label(buttonComposite, SWT.NONE);
      descriptionLine2Label.setText(descriptionLine2);
      return button;
    }
  }

  @Override
  public String getName() {
    return Messages.ScriptMappingTab_TAB_NAME;
  }

  protected List<? extends TabField<?, ?, Elements, Params>> getTabFields() {
    return TAB_FIELDS;
  }

  static final List<? extends TabField<?, ?, Elements, Params>> TAB_FIELDS;
  static {
    List<TabField<?, ?, Elements, Params>> list =
        new ArrayList<ChromiumRemoteTab.TabField<?, ?, Elements, Params>>(4);

    list.add(new TabField<String, LookupMode, Elements, Params>(
        LaunchParams.SOURCE_LOOKUP_MODE, TypedMethods.STRING,
        new FieldAccess<LookupMode, Elements>() {
          @Override
          void setValue(LookupMode value, Elements tabElements) {
            tabElements.getLookupMode().select(value);
          }
          @Override
          LookupMode getValue(Elements tabElements) {
            return tabElements.getLookupMode().getSelected();
          }
        },
        new DefaultsProvider<LookupMode, Params>() {
          @Override LookupMode getFallbackValue() {
            // TODO: support default value from eclipse variables.
            return LookupMode.DEFAULT_VALUE;
          }
          @Override LookupMode getInitialConfigValue(Params context) {
            return LookupMode.AUTO_DETECT;
          }
        },
        LookupMode.STRING_CONVERTER));

    TAB_FIELDS = Collections.unmodifiableList(list);
  }
}
