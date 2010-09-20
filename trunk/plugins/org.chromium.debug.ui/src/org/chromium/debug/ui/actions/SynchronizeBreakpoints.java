// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.BreakpointSynchronizer;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
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
    final Set<DebugTargetImpl> targets = new HashSet<DebugTargetImpl>(3);
    for (Iterator<?> it = structuredSelection.iterator(); it.hasNext(); ) {
      Object element = it.next();
      DebugTargetImpl debugTargetImpl = getDebugTargetImpl(element);
      if (debugTargetImpl == null) {
        continue;
      }
      targets.add(debugTargetImpl);
    }
    if (targets.isEmpty()) {
      return null;
    }
    if (direction != BreakpointSynchronizer.Direction.RESET_REMOTE && targets.size() > 1) {
      // Only "reset remote" mode is implemented for a multiple selection.
      return null;
    }

    return new Runnable() {
      public void run() {
        new Job(MessageFormat.format(Messages.SynchronizeBreakpoints_JOB_TITLE, targets.size())) {
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
            for (DebugTargetImpl target : targets) {
              target.synchronizeBreakpoints(direction, callback);
            }
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    };
  }

  private Runnable currentRunnable;

  public static DebugTargetImpl getDebugTargetImpl(Object element) {
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
    return (DebugTargetImpl) debugTarget;
  }
}
