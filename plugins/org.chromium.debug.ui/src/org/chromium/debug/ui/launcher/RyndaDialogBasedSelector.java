// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.Collections;

import org.chromium.debug.core.model.JavascriptVmEmbedderFactory;
import org.chromium.debug.ui.DialogUtils;
import org.chromium.debug.ui.DialogUtils.Gettable;
import org.chromium.debug.ui.DialogUtils.MessagePriority;
import org.chromium.debug.ui.DialogUtils.OkButtonControl;
import org.chromium.debug.ui.DialogUtils.OkButtonElements;
import org.chromium.debug.ui.DialogUtils.Optional;
import org.chromium.debug.ui.DialogUtils.Scope;
import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueProcessor;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.chromium.sdk.Browser.TabConnector;
import org.chromium.sdk.rynda.RyndaBrowser;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * A temporary dialog-based selector where user simply can enter tab id (a data he must know
 * from elsewhere).
 */
class RyndaDialogBasedSelector implements JavascriptVmEmbedderFactory.RyndaTabSelector {
  static final RyndaDialogBasedSelector INSTANCE = new RyndaDialogBasedSelector();

  private static final int DEFAULT_TAB_ID = 2;

  @Override
  public TabConnector selectTab(RyndaBrowser browser) {
    final Integer [] tabIdBuff = { null };
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        Integer tabId = new DialogImpl(shell, DEFAULT_TAB_ID).openAndGetResult();
        tabIdBuff[0] = tabId;
      }
    });
    if (tabIdBuff[0] == null) {
      return null;
    }
    return browser.getTabConnector(tabIdBuff[0].intValue());
  }

  private static class DialogImpl extends Dialog {

    private final int defaultTabId;
    private Integer result = null;
    private Logic logic = null;

    DialogImpl(Shell shell, int defaultTabId) {
      super(shell);
      this.defaultTabId = defaultTabId;
    }

    public Integer openAndGetResult() {
      open();
      return result;
    }

    @Override
    public void create() {
      super.create();
      logic.updateAll();
    }

    @Override
    protected void okPressed() {
      result = logic.getResult();
      super.okPressed();
    }

    @Override
    protected void configureShell(Shell shell) {
      super.configureShell(shell);
      shell.setText("Specify tab id");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite composite = (Composite) super.createDialogArea(parent);
      logic = createElementsAndLogic(composite);
      return composite;
    }

    private interface Elements extends OkButtonElements {
      Text getTabIdField();
    }

    private interface Logic {
      void updateAll();
      Integer getResult();
    }

    private Logic createElementsAndLogic(Composite composite) {

      Composite tabIdGroup = new Composite(composite, SWT.NONE);
      tabIdGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      tabIdGroup.setLayout(gridLayout);

      Label label = new Label(tabIdGroup, SWT.NONE);
      label.setText("Tab id:");

      final Text text = new Text(tabIdGroup, SWT.NONE);

      text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      Elements elements = new Elements() {
        @Override
        public Button getOkButton() {
          return getButton(IDialogConstants.OK_ID);
        }
        @Override
        public void setMessage(String message, int type) {
          // Ignore
        }

        @Override
        public Text getTabIdField() {
          return text;
        }
      };
      return createLogic(elements, defaultTabId);
    }

    private Logic createLogic(final Elements elements, int defaultTabId) {
      final Updater updater = new Updater();
      Scope rootScope = updater.rootScope();

      final Text tabIdTextField = elements.getTabIdField();

      final ValueSource<String> tabIdEditorSource = new ValueSource<String>() {
        {
          DialogUtils.addModifyListener(tabIdTextField, this, updater);
        }
        @Override
        public String getValue() {
          return tabIdTextField.getText();
        }
      };
      updater.addSource(rootScope, tabIdEditorSource);

      tabIdTextField.setText(String.valueOf(defaultTabId));
      tabIdTextField.setSelection(0, tabIdTextField.getText().length());

      ValueProcessor<Optional<Integer>> processedTabId = DialogUtils.createProcessor(
          new Gettable<Optional<Integer>>() {
        @Override
        public Optional<Integer> getValue() {
          String text = tabIdEditorSource.getValue();
          if (text.isEmpty()) {
            return DialogUtils.createErrorOptional(
                new DialogUtils.Message("Enter tab id", MessagePriority.BLOCKING_INFO));
          }
          int tabId;
          try {
            tabId = Integer.parseInt(text);
          } catch (NumberFormatException e) {
            return DialogUtils.createErrorOptional(new DialogUtils.Message(
                "Tab id should be numeric", MessagePriority.BLOCKING_PROBLEM));
          }
          return DialogUtils.createOptional(tabId);
        }
      });
      updater.addConsumer(rootScope, processedTabId);
      updater.addDependency(processedTabId, tabIdEditorSource);
      updater.addSource(rootScope, processedTabId);

      final OkButtonControl<Integer> okButtonControl = new OkButtonControl<Integer>(processedTabId,
          Collections.<ValueSource<String>>emptyList(), elements);
      updater.addConsumer(rootScope, okButtonControl);
      updater.addDependency(okButtonControl, okButtonControl.getDependencies());

      return new Logic() {
        @Override
        public void updateAll() {
          updater.updateAll();
        }

        @Override
        public Integer getResult() {
          return okButtonControl.getNormalValue();
        }
      };
    }
  }
}