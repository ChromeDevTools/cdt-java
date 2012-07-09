// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.WorkspaceBridge.BreakpointHandler;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.ExceptionCatchMode;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ExceptionBreakpoints implements IWorkbenchWindowActionDelegate {
  public static class All extends ExceptionBreakpoints {
    public All() {
      super(JavascriptVm.ExceptionCatchMode.ALL);
    }
  }
  public static class Uncaught extends ExceptionBreakpoints {
    public Uncaught() {
      super(JavascriptVm.ExceptionCatchMode.UNCAUGHT);
    }
  }
  public static class None extends ExceptionBreakpoints {
    public None() {
      super(JavascriptVm.ExceptionCatchMode.NONE);
    }
  }

  private ExceptionBreakpoints(ExceptionCatchMode catchMode) {
    this.catchMode = catchMode;
  }

  private final ExceptionCatchMode catchMode;
  private Performer currentPerformer = null;

  public void run(IAction action) {
    if (currentPerformer == null) {
      return;
    }
    currentPerformer.run(action);
    currentPerformer = null;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    currentPerformer = createPerformer(selection);
    if (currentPerformer == null) {
      action.setEnabled(false);
    } else {
      boolean checked = currentPerformer.getCurrentCheckedState();
      action.setChecked(checked);

      // Always enable, because disabled check looks unimpressive.
      boolean uiEnabled = true;
      action.setEnabled(uiEnabled);
    }
  }

  private Performer createPerformer(ISelection selection) {
    ConnectedTargetData targetData = getConnectedTargetData(selection);
    if (targetData == null) {
      return null;
    }
    final BreakpointHandler breakpointHandler =
        targetData.getWorkspaceRelations().getBreakpointHandler();
    final ExceptionCatchMode currentCatchMode = breakpointHandler.getBreakExceptionState();
    return new Performer() {
      @Override boolean getCurrentCheckedState() {
        return catchMode == currentCatchMode;
      }
      @Override void run(IAction action) {
        if (catchMode == currentCatchMode) {
          // We are enabled, but we are no-op.
          return;
        }
        ExceptionCatchMode newValue = catchMode;
        breakpointHandler.setBreakExceptionState(newValue);
      }
    };
  }

  private ConnectedTargetData getConnectedTargetData(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    if (structuredSelection.size() != 1) {
      return null;
    }
    return SynchronizeBreakpoints.getConnectionTargetData(structuredSelection.getFirstElement());
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  private static abstract class Performer {
    abstract void run(IAction action);
    abstract boolean getCurrentCheckedState();
  }
}
