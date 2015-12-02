// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions.pinpoint;

import org.chromium.debug.core.model.Value;
import org.chromium.debug.ui.ChromiumDebugUIPlugin;
import org.chromium.debug.ui.DialogUtils.OkButtonElements;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Action main dialog. User enters a property name of the global object (where the value gets
 * saved to) and additionally checks whether he wants preview and whether he wants
 * to automatically add corresponding watch expression.
 * The dialog logic is external to this class.
 */
class DialogImpl extends TitleAreaDialog {

  /**
   * UI elements of the dialog that are important for the dialog logic.
   */
  interface Elements extends OkButtonElements {
    Text getExpressionText();

    Button getPreviewCheckBox();

    /** Preview report shows here. */
    Label getPreviewDisplay();

    Button getAddWatchCheckBox();

    /**
     * A shell for opening additional dialog windows.
     */
    Shell getParentShell();
  }

  /**
   * Typed API to dialog settings persistence.
   */
  interface DialogPreferencesStore {
    String getExpressionText();
    void setExpressionText(String propertyName);
    boolean getPreviewCheck();
    void setPreviewCheck(boolean check);
    boolean getAddWatchExpression();
    void setAddWatchExpression(boolean value);
  }

  private static final String DIALOG_SETTINGS_KEY = "PinPointValueDialog"; //$NON-NLS-1$

  private final Value uiValue;
  private DialogLogic.Handle logic = null;

  DialogImpl(Shell shell, Value uiValue) {
    super(shell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.uiValue = uiValue;
  }

  @Override
  public void create() {
    super.create();
    logic.updateAll();
  }

  @Override
  protected void okPressed() {
    Runnable okRunnable  = logic.getOkRunnable();
    if (okRunnable == null) {
      return;
    }
    logic.saveStateInStore();
    okRunnable.run();
    super.okPressed();
  }

  /**
   * Creates dialog elements and initializes logic implementation.
   */
  @Override
  protected Control createDialogArea(Composite ancestor) {
    getShell().setText(Messages.DialogImpl_WINDOW_TITLE);
    setTitle(Messages.DialogImpl_WINDOW_SUBTITLE);

    Composite parent = new Composite(ancestor, SWT.NULL);
    {
      GridLayout topLayout = new GridLayout();
      topLayout.numColumns = 1;
      parent.setLayout(topLayout);
      parent.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    // Property expression part.

    final Text propertyExpressionEditor;
    {
      Label propertyExpressionPrompt = new Label(parent, 0);
      propertyExpressionPrompt.setText(
          Messages.DialogImpl_EXPRESSION_PROMPT);

      Composite propertyExpressionComposite = new Composite(parent, SWT.NONE);
      GridLayout topLayout = new GridLayout();
      topLayout.numColumns = 2;
      topLayout.horizontalSpacing = 0;
      propertyExpressionComposite.setLayout(topLayout);
      propertyExpressionComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      Label globalObjectLabel = new Label(propertyExpressionComposite, 0);
      globalObjectLabel.setText(Messages.DialogImpl_GLOBAL_MARK);

      propertyExpressionEditor = new Text(propertyExpressionComposite, SWT.SINGLE | SWT.BORDER);
      propertyExpressionEditor.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    // Preview part.

    final Button previewCheckBox;
    final Label previewDisplayText;
    {
      Group checkValueGroup = new Group(parent, SWT.NONE);
      GridLayout topLayout = new GridLayout();
      topLayout.numColumns = 1;
      checkValueGroup.setLayout(topLayout);
      checkValueGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

      previewCheckBox = new Button(checkValueGroup, SWT.CHECK);
      previewCheckBox.setText(Messages.DialogImpl_PROPERTY_PREVIEW);

      previewDisplayText = new Label(checkValueGroup, SWT.WRAP);
      previewDisplayText.setLayoutData(new GridData(GridData.FILL_BOTH));
      previewDisplayText.setText("\n\n\n"); //$NON-NLS-1$
    }

    // "Add expression" checkbox.

    final Button addWatchCheckBox = new Button(parent, SWT.CHECK);
    addWatchCheckBox.setText(Messages.DialogImpl_ADD_WATCH);

    // Getters to all logic-essential UI elements.
    Elements elements = new Elements() {
      @Override public Shell getParentShell() {
        return DialogImpl.this.getParentShell();
      }

      @Override public Button getOkButton() {
        return DialogImpl.this.getButton(IDialogConstants.OK_ID);
      }

      @Override public void setMessage(String message, int type) {
        DialogImpl.this.setMessage(message, type);
      }

      @Override public Text getExpressionText() {
        return propertyExpressionEditor;
      }

      @Override public Button getPreviewCheckBox() {
        return previewCheckBox;
      }

      @Override public Label getPreviewDisplay() {
        return previewDisplayText;
      }

      @Override public Button getAddWatchCheckBox() {
        return addWatchCheckBox;
      }
    };

    // Settings store for user last entered text (raw storage).
    final IDialogSettings dialogSection;
    {
      IDialogSettings pluginSettings = ChromiumDebugUIPlugin.getDefault().getDialogSettings();
      IDialogSettings dialogSectionVar = pluginSettings.getSection(DIALOG_SETTINGS_KEY);
      if (dialogSectionVar == null) {
        dialogSectionVar = pluginSettings.addNewSection(DIALOG_SETTINGS_KEY);
      }
      dialogSection = dialogSectionVar;
    }

    // Settings store wrapped as typed interface.
    DialogPreferencesStore preferencesStore = new DialogPreferencesStore() {
      private static final String EXPRESSION = "expression"; //$NON-NLS-1$
      private static final String SHOW_PREVIEW = "showPreview"; //$NON-NLS-1$
      private static final String ADD_WATCH_EXPRESSION = "addWatchExpression"; //$NON-NLS-1$
      private static final String defaultExpressionText = "."; //$NON-NLS-1$

      @Override
      public String getExpressionText() {
        String value = dialogSection.get(EXPRESSION);
        if (value == null) {
          value = defaultExpressionText;
        }
        return value;
      }

      @Override public void setExpressionText(String expressionText) {
        dialogSection.put(EXPRESSION, expressionText);
      }

      @Override public boolean getPreviewCheck() {
        return dialogSection.getBoolean(SHOW_PREVIEW);
      }

      @Override public void setPreviewCheck(boolean check) {
        dialogSection.put(SHOW_PREVIEW, check);
      }

      @Override public boolean getAddWatchExpression() {
        return dialogSection.getBoolean(ADD_WATCH_EXPRESSION);
      }

      @Override public void setAddWatchExpression(boolean value) {
        dialogSection.put(ADD_WATCH_EXPRESSION, value);
      }
    };

    // Build a dialog logic.
    logic = DialogLogic.buildDialogLogic(elements, preferencesStore, uiValue);
    return parent;

  }
}