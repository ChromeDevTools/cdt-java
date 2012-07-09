// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;


/**
 * A small set of utility classes that help programming progress monitor logic.
 */
public class ProgressUtil {

  /**
   * Represents one stage of the whole process with a name and relative length called "weight".
   * Knowing all stages in advance helps calculate their ticks for {@link IProgressMonitor}
   * (this process is called layout here).
   */
  public static class Stage {
    private final String name;
    private final float weight;
    private int workNumberForMonitor = 0;

    public Stage(String name, float weight) {
      this.name = name;
      this.weight = weight;
    }

    public void start(MonitorWrapper monitorWrapper) {
      monitorWrapper.setTaskName(name);
    }

    public void finish(MonitorWrapper monitorWrapper) {
      monitorWrapper.worked(workNumberForMonitor);
      monitorWrapper.clearTaskName();
    }

    public MonitorWrapper createSubMonitorWrapper(MonitorWrapper monitorWrapper) {
      return monitorWrapper.createSubMonitorWrapper(name, workNumberForMonitor);
    }
    public IProgressMonitor createSubMonitor(MonitorWrapper monitorWrapper) {
      return monitorWrapper.createSubMonitor(workNumberForMonitor);
    }
  }

  /**
   * From a list of all stages of some process calculates their tick numbers
   * for {@link IProgressMonitor} according to their weights. The whole process
   * should be {@link #LAYOUT_TOTAL_STEPS} in ticks.
   * Method has a return value so that it could be called from expressions (notably,
   * constant initializer of an interface so that layout could be performed in initialization
   * phase).
   * @return always true
   */
  public static boolean layoutProgressPlan(Stage ... stages) {
    float totalWeight = 0;
    for (Stage stage : stages) {
      totalWeight += stage.weight;
    }
    float sum = 0;
    int steps = 0;
    for (Stage stage : stages) {
      sum += stage.weight;
      int nextSteps = (int)(sum / totalWeight * LAYOUT_TOTAL_STEPS);
      stage.workNumberForMonitor = nextSteps - steps;
      steps = nextSteps;
    }
    return true;
  }

  /**
   * A small wrapper around the {@link IProgressMonitor} that keeps a default label -- the string
   * to show when a current stage has no label of its own.
   */
  public static class MonitorWrapper {
    private final IProgressMonitor progressMonitor;
    private final String defaultLabel;

    public MonitorWrapper(IProgressMonitor progressMonitor, String defaultLabel) {
      this.progressMonitor = progressMonitor;
      this.defaultLabel = defaultLabel;
    }

    public void beginTask() {
      progressMonitor.beginTask(defaultLabel, LAYOUT_TOTAL_STEPS);
    }

    public void setTaskName(String name) {
      if (name == null) {
        name = defaultLabel;
      }
      progressMonitor.setTaskName(name);
    }

    public void clearTaskName() {
      progressMonitor.setTaskName(defaultLabel);
    }

    public void worked(int work) {
      progressMonitor.worked(work);
    }

    public MonitorWrapper createSubMonitorWrapper(String label, int steps) {
      final SubProgressMonitor subProgressMonitor = new SubProgressMonitor(progressMonitor, steps);
      return new MonitorWrapper(subProgressMonitor, label);
    }

    public IProgressMonitor createSubMonitor(int steps) {
      return new SubProgressMonitor(progressMonitor, steps);
    }

    public void done() {
      clearTaskName();
      progressMonitor.done();
    }

    public boolean isCanceled() {
      return progressMonitor.isCanceled();
    }
  }

  private static final int LAYOUT_TOTAL_STEPS = 1000;
}
