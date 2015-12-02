// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ProgressUtil;
import org.chromium.debug.core.util.ProgressUtil.MonitorWrapper;
import org.chromium.debug.core.util.ProgressUtil.Stage;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.RelayOk;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;

/**
 * Does several things we need on debug session start. This procedure works asynchronously while
 * user may already start his debug activity.
 */
class LaunchInitializationProcedure {
  static void startAsync(VProjectWorkspaceBridge workspaceBridge,
      BreakpointSynchronizer.Direction presetDirection) {
    final LaunchInitializationProcedure procedure =
        new LaunchInitializationProcedure(workspaceBridge, presetDirection);

    ILaunch launch = workspaceBridge.getConnectedTargetData().getDebugTarget().getLaunch();
    final String jobName = NLS.bind(Messages.LaunchInitializationProcedure_JOB_NAME,
        launch.getLaunchConfiguration().getName());
    Job job = new Job(jobName) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        return procedure.execute(monitor);
      }
    };
    job.schedule();
  }

  private final VProjectWorkspaceBridge workspaceBridge;
  private final BreakpointSynchronizer.Direction presetDirection;

  private LaunchInitializationProcedure(VProjectWorkspaceBridge workspaceBridge,
      BreakpointSynchronizer.Direction presetDirection) {
    this.workspaceBridge = workspaceBridge;
    this.presetDirection = presetDirection;
  }

  /**
   * A list of tasks for progress monitor.
   */
  private interface WorkPlan {
    Stage PREINIT = new Stage(Messages.LaunchInitializationProcedure_UPDATE_DEBUGGER_STATE, 0.1f);
    Stage SET_OPTIONS = new Stage(Messages.LaunchInitializationProcedure_SET_OPTIONS, 1f);
    Stage LOAD_SCRIPTS = new Stage(Messages.LaunchInitializationProcedure_LOAD_SCRIPTS, 1f);
    Stage SYNCHRONIZE_BREAKPOINTS =
        new Stage(Messages.LaunchInitializationProcedure_SYNCHRONIZE_BREAKPOINTS, 1f);

    boolean IS_INITIZALIZED = ProgressUtil.layoutProgressPlan(PREINIT, SET_OPTIONS, LOAD_SCRIPTS,
        SYNCHRONIZE_BREAKPOINTS);
  }

  private IStatus execute(IProgressMonitor monitor) {
    ConnectedTargetData connectedTargetData = workspaceBridge.getConnectedTargetData();

    MonitorWrapper monitorWrapper = new MonitorWrapper(monitor, ""); //$NON-NLS-1$

    monitorWrapper.beginTask();
    try {
      WorkPlan.PREINIT.start(monitorWrapper);
      // Nothing here right now.
      WorkPlan.PREINIT.finish(monitorWrapper);
      checkIsCanceled(monitorWrapper);
      WorkPlan.SET_OPTIONS.start(monitorWrapper);
      // Not implemented yet
      WorkPlan.SET_OPTIONS.finish(monitorWrapper);
      checkIsCanceled(monitorWrapper);
      WorkPlan.LOAD_SCRIPTS.start(monitorWrapper);
      workspaceBridge.reloadScriptsAtStart();
      WorkPlan.LOAD_SCRIPTS.finish(monitorWrapper);
      checkIsCanceled(monitorWrapper);
      synchronizeBreakpoints(
          WorkPlan.SYNCHRONIZE_BREAKPOINTS.createSubMonitorWrapper(monitorWrapper));

    } finally {
      monitorWrapper.done();
    }
    return Status.OK_STATUS;
  }

  /**
   * A list of tasks for breakpoint synchronization subprocess.
   */
  private interface BreakpointsWorkPlan {
    Stage ANALYZE = new Stage(null, 1f);
    Stage REMOTE_CHANGES = new Stage(null, 1f);

    boolean IS_LAYOUTED = ProgressUtil.layoutProgressPlan(ANALYZE, REMOTE_CHANGES);
  }

  private void synchronizeBreakpoints(MonitorWrapper monitor) {
    monitor.beginTask();
    try {
      BreakpointsWorkPlan.ANALYZE.start(monitor);

      DebugTargetImpl debugTarget = workspaceBridge.getConnectedTargetData().getDebugTarget();
      ILaunchConfiguration launchConfiguration = debugTarget.getLaunch().getLaunchConfiguration();

      BreakpointSynchronizer.Direction direction;
      if (presetDirection == null) {
        try {
          direction = LaunchParams.readBreakpointSyncDirection(launchConfiguration);
        } catch (CoreException e) {
          ChromiumDebugPlugin.log(
              new Exception("Failed to read breakpoint synchronization direction " + //$NON-NLS-1$
                  "from launch configuration " + launchConfiguration.getName(), e)); //$NON-NLS-1$
          direction = null;
        }
      } else {
        direction = presetDirection;
      }

      if (direction == null) {
        return;
      }
      final CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
      BreakpointSynchronizer.Callback callback = new BreakpointSynchronizer.Callback() {
        public void onDone(IStatus status) {
          callbackSemaphore.callbackDone(null);
        }
      };
      workspaceBridge.getBreakpointSynchronizer().syncBreakpoints(direction, callback);
      RelayOk relayOk = SYNCHRONIZER_MUST_RELAY_OK;
      checkIsCanceled(monitor);

      BreakpointsWorkPlan.ANALYZE.finish(monitor);

      BreakpointsWorkPlan.REMOTE_CHANGES.start(monitor);
      callbackSemaphore.tryAcquireDefault(relayOk);
      BreakpointsWorkPlan.REMOTE_CHANGES.finish(monitor);

    } finally {
      monitor.done();
    }
  }

  private static void checkIsCanceled(MonitorWrapper monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  private static final RelayOk SYNCHRONIZER_MUST_RELAY_OK = new RelayOk() {};
}
