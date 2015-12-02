// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ChromiumExceptionBreakpoint;
import org.chromium.debug.core.model.VProjectWorkspaceBridge;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Adds new exception breakpoint. It doesn't really make sense to have more than 2 such breakpoints
 * (one for uncaught and one for all breakpoints to be able to disable/enable them), but
 * there is no elegant way of limiting this.
 */
public class AddExceptionBreakpointAction implements IWorkbenchWindowActionDelegate {

  private Performer currentPerformer = null;

  @Override
  public void run(IAction action) {
    if (currentPerformer == null) {
      return;
    }
    currentPerformer.run();
    currentPerformer = null;
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    currentPerformer = createPerformer(selection);
  }

  private Performer createPerformer(ISelection selection) {
    final IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    return new Performer() {
      @Override
      void run() {
        IResource resource = ResourcesPlugin.getWorkspace().getRoot();
        boolean includingCaught = true;
        try {
          ChromiumExceptionBreakpoint exceptionBreakpoint = new ChromiumExceptionBreakpoint(
              resource, includingCaught, VProjectWorkspaceBridge.DEBUG_MODEL_ID);
          breakpointManager.addBreakpoint(exceptionBreakpoint);
        } catch (CoreException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  @Override
  public void dispose() {
  }

  @Override
  public void init(IWorkbenchWindow window) {
  }

  private abstract class Performer {
    abstract void run();
  }
}
