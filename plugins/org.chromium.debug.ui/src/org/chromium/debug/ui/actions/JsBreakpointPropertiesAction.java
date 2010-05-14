// Copyright (c) 20109 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ChromiumLineBreakpoint;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.dialogs.PropertyDialogAction;

/**
 * Action to bring up the breakpoint properties dialog.
 */
public class JsBreakpointPropertiesAction implements IObjectActionDelegate {

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


  private Runnable createRunnable(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    if (structuredSelection.size() != 1) {
      return null;
    }
    Object element = structuredSelection.getFirstElement();
    if (element instanceof ChromiumLineBreakpoint == false) {
      return null;
    }
    final ChromiumLineBreakpoint breakpoint = (ChromiumLineBreakpoint) element;

    return new Runnable() {
      public void run() {
        runAction(breakpoint, site);
      }
    };
  }

  protected static void runAction(final IBreakpoint breakpoint, IShellProvider shell) {
    PropertyDialogAction action =
      new PropertyDialogAction(shell,
          new ISelectionProvider() {
            public void addSelectionChangedListener(ISelectionChangedListener listener) {
            }

            public ISelection getSelection() {
              return new StructuredSelection(breakpoint);
            }

            public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            }

            public void setSelection(ISelection selection) {
            }
          });
    action.run();
  }
}
