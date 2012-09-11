// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ChromiumExceptionBreakpoint;
import org.chromium.debug.core.model.ChromiumLineBreakpoint;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Action to bring up the breakpoint properties dialog.
 */
public abstract class JsBreakpointPropertiesAction implements IObjectActionDelegate {

  public static class Line extends JsBreakpointPropertiesAction {
    @Override protected boolean isCorrectType(IBreakpoint breakpoint) {
      return breakpoint instanceof ChromiumLineBreakpoint;
    }
  }

  public static class Exception extends JsBreakpointPropertiesAction {
    @Override protected boolean isCorrectType(IBreakpoint breakpoint) {
      return breakpoint instanceof ChromiumExceptionBreakpoint;
    }
  }

  private Runnable currentRunnable;
  private IWorkbenchPartSite site = null;

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    site = targetPart.getSite();
  }

  public void run(IAction action) {
    currentRunnable.run();
  }

  public void selectionChanged(IAction action, ISelection selection) {
    currentRunnable = createRunnable(selection);
    action.setEnabled(currentRunnable != null);
  }

  protected abstract boolean isCorrectType(IBreakpoint breakpoint);

  private Runnable createRunnable(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    if (structuredSelection.size() != 1) {
      return null;
    }
    Object element = structuredSelection.getFirstElement();
    if (element instanceof IBreakpoint == false) {
      return null;
    }
    final IBreakpoint breakpoint = (IBreakpoint) element;
    if (!isCorrectType(breakpoint)) {
      return null;
    }

    return new Runnable() {
      public void run() {
        runAction(breakpoint, site);
      }
    };
  }

  protected static void runAction(IBreakpoint breakpoint, IShellProvider shell) {
    PreferenceDialog propertyDialog = PreferencesUtil.createPropertyDialogOn(
        shell.getShell(), breakpoint, (String) null, (String[]) null, null);

    propertyDialog.open();
  }
}
