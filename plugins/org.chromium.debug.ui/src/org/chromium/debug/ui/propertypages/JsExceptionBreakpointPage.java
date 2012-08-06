// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.propertypages;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.ChromiumExceptionBreakpoint;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A JavaScript line breakpoint property page.
 */
public class JsExceptionBreakpointPage extends PropertyPage {
  private Button enabledCheckbox;

  private Button includeCaughtCheckbox;

  @Override
  protected Control createContents(Composite parent) {
    noDefaultAndApplyButton();
    Composite mainComposite = JsLineBreakpointPage.createComposite(parent, 2, 1);
    try {
      createBreakpointDataControls(mainComposite);
      createInfoControls(mainComposite);
      createEnabledControls(mainComposite);
      createIncludeCaughtControls(mainComposite);
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
    ChromiumExceptionBreakpoint breakpoint = getBreakpoint();
    breakpoint.setEnabled(enabledCheckbox.getSelection());
    breakpoint.setIncludeCaught(includeCaughtCheckbox.getSelection());
  }

  private void createBreakpointDataControls(Composite mainComposite) {
  }

  private void createInfoControls(Composite parent) {
  }

  private void createEnabledControls(Composite parent) throws CoreException {
    enabledCheckbox = checkboxControl(parent,
        Messages.JavascriptLineBreakpointPage_Enabled, getBreakpoint().isEnabled());
  }

  private void createIncludeCaughtControls(Composite parent) throws CoreException {
    Button includeUncaughtCheckbox = checkboxControl(parent,
        Messages.JsExceptionBreakpointPage_UNCAUGHT, true);
    includeUncaughtCheckbox.setEnabled(false);

    includeCaughtCheckbox = checkboxControl(parent,
        Messages.JsExceptionBreakpointPage_CAUGHT, getBreakpoint().getIncludeCaught());
  }

  private Button checkboxControl(Composite parent, String label, boolean initialState)
      throws CoreException {
    Button checkbox = new Button(parent, SWT.CHECK);
    GridData gd = new GridData();
    gd.horizontalSpan = 2;
    checkbox.setLayoutData(gd);
    checkbox.setSelection(initialState);
    checkbox.setText(label);
    return checkbox;
  }

  @Override
  public void setErrorMessage(String newMessage) {
    super.setErrorMessage(newMessage);
    setValid(newMessage == null);
  }

  protected ChromiumExceptionBreakpoint getBreakpoint() {
    return (ChromiumExceptionBreakpoint) getElement();
  }
}
