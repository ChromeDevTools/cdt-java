// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.RunningTargetData;
import org.chromium.debug.core.model.WorkspaceBridge.BreakpointHandler;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.ExceptionCatchType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ExceptionBreakpoints implements IWorkbenchWindowActionDelegate {
  public static class Caught extends ExceptionBreakpoints {
    public Caught() {
      super(JavascriptVm.ExceptionCatchType.CAUGHT);
    }
  }
  public static class Uncaught extends ExceptionBreakpoints {
    public Uncaught() {
      super(JavascriptVm.ExceptionCatchType.UNCAUGHT);
    }
  }

  private ExceptionBreakpoints(ExceptionCatchType catchType) {
    this.catchType = catchType;
  }

  private final JavascriptVm.ExceptionCatchType catchType;
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
    action.setEnabled(currentPerformer != null);
    if (currentPerformer != null) {
      action.setChecked(currentPerformer.getCurrentCheckedState());
    }
  }

  private Performer createPerformer(ISelection selection) {
    RunningTargetData targetData = getRunningTargetData(selection);
    if (targetData == null) {
      return null;
    }
    final BreakpointHandler breakpointHandler =
        targetData.getWorkspaceRelations().getBreakpointHandler();
    final Boolean state = breakpointHandler.getBreakExceptionState(catchType);
    return new Performer() {
      @Override boolean getCurrentCheckedState() {
        return state == Boolean.TRUE;
      }
      @Override void run(IAction action) {
        boolean newValue = !getCurrentCheckedState();
        breakpointHandler.setBreakExceptionState(catchType, newValue);
      }
    };
  }

  private RunningTargetData getRunningTargetData(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    if (structuredSelection.size() != 1) {
      return null;
    }
    return SynchronizeBreakpoints.getRunningTargetData(structuredSelection.getFirstElement());
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
