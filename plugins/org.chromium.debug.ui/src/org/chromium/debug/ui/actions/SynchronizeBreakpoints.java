// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.BreakpointSynchronizer;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
import org.chromium.debug.core.model.ConnectedTargetData;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class SynchronizeBreakpoints implements IWorkbenchWindowActionDelegate {

  public static class ResetRemote extends SynchronizeBreakpoints {
    public ResetRemote() {
      super(BreakpointSynchronizer.Direction.RESET_REMOTE);
    }
  }

  public static class ResetLocal extends SynchronizeBreakpoints {
    public ResetLocal() {
      super(BreakpointSynchronizer.Direction.RESET_LOCAL);
    }
  }

  public static class Merge extends SynchronizeBreakpoints {
    public Merge() {
      super(BreakpointSynchronizer.Direction.MERGE);
    }
  }

  private final BreakpointSynchronizer.Direction direction;

  protected SynchronizeBreakpoints(Direction direction) {
    this.direction = direction;
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  public void run(IAction action) {
    if (currentRunnable == null) {
      return;
    }
    currentRunnable.run();
    currentRunnable = null;
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
    final Set<ConnectedTargetData> targetDatas = new HashSet<ConnectedTargetData>(3);
    for (Iterator<?> it = structuredSelection.iterator(); it.hasNext(); ) {
      Object element = it.next();
      ConnectedTargetData connectedTargetData = getConnectionTargetData(element);
      if (connectedTargetData == null) {
        continue;
      }
      targetDatas.add(connectedTargetData);
    }
    if (targetDatas.isEmpty()) {
      return null;
    }
    if (direction != BreakpointSynchronizer.Direction.RESET_REMOTE && targetDatas.size() > 1) {
      // Only "reset remote" mode is implemented for a multiple selection.
      return null;
    }

    return new Runnable() {
      public void run() {
        new Job(MessageFormat.format(Messages.SynchronizeBreakpoints_JOB_TITLE,
            targetDatas.size())) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            // TODO(peter.rybin): consider blocking this method until callback is invoked to
            // keep the UI jobs open while something is still happening.
            BreakpointSynchronizer.Callback callback = new BreakpointSynchronizer.Callback() {
              public void onDone(IStatus status) {
                ChromiumDebugPlugin.log(status);
              }
            };

            // TODO(peter.rybin): consider showing progress for several targets.
            for (ConnectedTargetData data : targetDatas) {
              data.synchronizeBreakpoints(direction, callback);
            }
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    };
  }

  private Runnable currentRunnable;

  public static ConnectedTargetData getConnectionTargetData(Object element) {
    IDebugTarget debugTarget;
    if (element instanceof ILaunch) {
      ILaunch launch = (ILaunch) element;
      debugTarget = launch.getDebugTarget();
    } else if (element instanceof IDebugElement) {
      IDebugElement debugElement = (IDebugElement) element;
      debugTarget = debugElement.getDebugTarget();
    } else {
      return null;
    }
    if (debugTarget instanceof DebugTargetImpl == false) {
      return null;
    }
    DebugTargetImpl debugTargetImpl = (DebugTargetImpl) debugTarget;
    return debugTargetImpl.getConnectedOrNull();
  }
}
