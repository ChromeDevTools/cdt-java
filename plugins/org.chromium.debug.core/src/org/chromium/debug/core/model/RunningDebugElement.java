// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.eclipse.debug.core.model.IDebugElement;

/**
 * A generic implementation of {@link IDebugElement} that corresponds to running
 * debug target. It contains a reference to the corresponding {@link RunningTargetData}
 * object.
 */
public class RunningDebugElement extends DebugElementImpl {
  private RunningTargetData runningData;

  public RunningDebugElement(RunningTargetData runningData) {
    super(runningData.getDebugTarget());
    this.runningData = runningData;
  }

  public RunningTargetData getRunningData() {
    return runningData;
  }
}