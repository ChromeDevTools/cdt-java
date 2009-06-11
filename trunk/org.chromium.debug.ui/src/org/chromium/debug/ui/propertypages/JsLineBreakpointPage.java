// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.propertypages;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.ChromiumLineBreakpoint;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A Javascript line breakpoint property page.
 */
public class JsLineBreakpointPage extends PropertyPage {

  private final List<String> errorMessages = new ArrayList<String>(2);

  private Button enabledCheckbox;

  private Button ignoreCountCheckbox;

  private Text ignoreCountText;

  private Button conditionCheckbox;

  private Text conditionText;

  @Override
  protected Control createContents(Composite parent) {
    noDefaultAndApplyButton();
    Composite mainComposite = createComposite(parent, 2, 1);
    try {
      createBreakpointDataControls(mainComposite);
      createInfoControls(mainComposite);
      createEnabledControls(mainComposite);
      createIgnoreCountControls(mainComposite);
      createConditionControls(mainComposite);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
    setValid(true);
    return mainComposite;
  }

  @Override
  public boolean performOk() {
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        storePrefs();
      }
    };
    try {
      ResourcesPlugin.getWorkspace().run(runnable, null, 0, null);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
    return super.performOk();
  }

  private void storePrefs() throws CoreException {
    ChromiumLineBreakpoint breakpoint = getBreakpoint();
    breakpoint.setEnabled(enabledCheckbox.getSelection());
    breakpoint.setIgnoreCount(ignoreCountCheckbox.getSelection()
        ? Integer.valueOf(ignoreCountText.getText())
        : -1);
    String condition = null;
    if (conditionCheckbox.getSelection()) {
      String text = conditionText.getText().trim();
      if (text.length() > 0) {
        condition = text;
      }
    }
    breakpoint.setCondition(condition);
  }

  private void createBreakpointDataControls(Composite mainComposite) {
    // new Label
  }

  private void createInfoControls(Composite parent) {
    Composite infoComposite = createComposite(parent, 2, 2);
    Label resourceLabel = new Label(infoComposite, SWT.NONE);
    resourceLabel.setText(Messages.JsLineBreakpointPage_ResourceLabel);
    Label resourceNameLabel = new Label(infoComposite, SWT.NONE);
    resourceNameLabel.setText(getBreakpoint().getMarker().getResource().getName());

    Label lineNumberLabel = new Label(infoComposite, SWT.NONE);
    lineNumberLabel.setText(Messages.JsLineBreakpointPage_LineNumberLabel);
    Label lineNumberValueLabel = new Label(infoComposite, SWT.NONE);
    String lineNumber = Messages.JsLineBreakpointPage_UnknownLineNumber;
    try {
      lineNumber = String.valueOf(getBreakpoint().getLineNumber());
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
    lineNumberValueLabel.setText(lineNumber);
  }

  private void createEnabledControls(Composite parent) throws CoreException {
    enabledCheckbox = new Button(parent, SWT.CHECK);
    GridData gd = new GridData();
    gd.horizontalSpan = 2;
    enabledCheckbox.setLayoutData(gd);
    enabledCheckbox.setSelection(getBreakpoint().isEnabled());
    enabledCheckbox.setText(Messages.JavascriptLineBreakpointPage_Enabled);
  }

  private void createIgnoreCountControls(Composite parent) throws CoreException {
    ignoreCountCheckbox = new Button(parent, SWT.CHECK);
    Integer ignoreCount = getBreakpoint().getIgnoreCount();
    ignoreCountCheckbox.setSelection(ignoreCount != null);
    ignoreCountCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ignoreCountText.setEnabled(ignoreCountCheckbox.getSelection());
        ignoreCountChanged();
      }
    });
    ignoreCountCheckbox.setText(Messages.JavascriptLineBreakpointPage_IgnoreCount);
    ignoreCountText = new Text(parent, SWT.SINGLE | SWT.BORDER);
    ignoreCountText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    ignoreCountText.setTextLimit(10);
    ignoreCountText.setEnabled(ignoreCountCheckbox.getSelection());
    if (ignoreCount != null) {
      ignoreCountText.setText(String.valueOf(ignoreCount));
    }
    ignoreCountText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        ignoreCountChanged();
      }
    });
  }

  private void ignoreCountChanged() {
    boolean isOn = ignoreCountCheckbox.getSelection();
    if (!isOn) {
      removeErrorMessage(Messages.JavascriptLineBreakpointPage_IgnoreCountErrorMessage);
      return;
    }
    String value = ignoreCountText.getText();
    int ignoreCount = -1;
    if (!ChromiumDebugPluginUtil.isInteger(value)) {
      addErrorMessage(Messages.JavascriptLineBreakpointPage_IgnoreCountErrorMessage);
      return;
    }
    ignoreCount = Integer.valueOf(value);
    if (ignoreCount < 1) {
      addErrorMessage(Messages.JavascriptLineBreakpointPage_IgnoreCountErrorMessage);
    } else {
      removeErrorMessage(Messages.JavascriptLineBreakpointPage_IgnoreCountErrorMessage);
    }
  }

  private void addErrorMessage(String message) {
    errorMessages.remove(message);
    errorMessages.add(message);
    setErrorMessage(message);
  }

  private void removeErrorMessage(String message) {
    errorMessages.remove(message);
    if (errorMessages.isEmpty()) {
      setErrorMessage(null);
    } else {
      setErrorMessage(errorMessages.get(errorMessages.size() - 1));
    }
  }

  @Override
  public void setErrorMessage(String newMessage) {
    super.setErrorMessage(newMessage);
    setValid(newMessage == null);
  }

  private void createConditionControls(Composite parent) {
    conditionCheckbox = new Button(parent, SWT.CHECK);
    conditionCheckbox.setSelection(getBreakpoint().getCondition() != null);
    conditionCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        conditionText.setEnabled(conditionCheckbox.getSelection());
        conditionChanged();
      }
    });
    conditionCheckbox.setText(Messages.JavascriptLineBreakpointPage_EnableCondition);
    GridData gd = new GridData();
    gd.horizontalSpan = 2;
    conditionCheckbox.setLayoutData(gd);
    conditionText = new Text(parent, SWT.MULTI | SWT.BORDER);
    gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    conditionText.setLayoutData(gd);
    conditionText.setTextLimit(300);
    conditionText.setFont(JFaceResources.getTextFont());
    conditionText.setEnabled(conditionCheckbox.getSelection());
    conditionText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        conditionChanged();
      }
    });
    conditionText.setText(maskNull(getBreakpoint().getCondition()));
  }

  private static String maskNull(String value) {
    return value == null
        ? "" : value; //$NON-NLS-1$
  }

  private void conditionChanged() {
    boolean isOn = conditionCheckbox.getSelection();
    if (!isOn) {
      removeErrorMessage(Messages.JavascriptLineBreakpointPage_BreakpointConditionErrorMessage);
      return;
    }
    String value = conditionText.getText();
    if (value == null) {
      addErrorMessage(Messages.JavascriptLineBreakpointPage_BreakpointConditionErrorMessage);
    } else {
      removeErrorMessage(Messages.JavascriptLineBreakpointPage_BreakpointConditionErrorMessage);
    }
  }

  private Composite createComposite(Composite parent, int columns, int horizontalSpan) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(columns, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setFont(parent.getFont());
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = horizontalSpan;
    composite.setLayoutData(gridData);
    return composite;
  }

  protected ChromiumLineBreakpoint getBreakpoint() {
    return (ChromiumLineBreakpoint) getElement();
  }
}
