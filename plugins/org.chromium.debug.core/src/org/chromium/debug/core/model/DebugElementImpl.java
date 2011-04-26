// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.model.JavascriptThread.SuspendedState;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;

/**
 * A generic IDebugElement implementation. It holds a familiy of more specialized
 * base classes.
 */
public abstract class DebugElementImpl extends PlatformObject implements IDebugElement {
  public abstract DebugTargetImpl getDebugTarget();

  public ILaunch getLaunch() {
    return getDebugTarget().getLaunch();
  }

  public String getModelIdentifier() {
    return getDebugTarget().getChromiumModelIdentifier();
  }

  @Override
  public Object getAdapter(Class adapter) {
    if (adapter == IDebugElement.class) {
      return this;
    }
    return super.getAdapter(adapter);
  }

  /**
   * An abstract base class for debug element that refers to {@link RunningTargetData}.
   * It declares no data field.
   */
  public static abstract class WithRunningBase extends DebugElementImpl {
    @Override
    public DebugTargetImpl getDebugTarget() {
      return getRunningData().getDebugTarget();
    }

    public abstract RunningTargetData getRunningData();
  }

  /**
   * A base class for debug element that refers to {@link RunningTargetData}.
   */
  public static class WithRunning extends WithRunningBase {
    private final RunningTargetData runningTargetData;

    public WithRunning(RunningTargetData runningTargetData) {
      this.runningTargetData = runningTargetData;
    }

    @Override
    public RunningTargetData getRunningData() {
      return runningTargetData;
    }
  }

  /**
   * An abstract base class for debug element that refers to
   * {@link JavascriptThread.SuspendedState}. It declares no data field.
   */
  public static abstract class WithSuspendedBase extends WithRunningBase {
    public RunningTargetData getRunningData() {
      return getSuspendedState().getRunningTargetData();
    }

    public abstract JavascriptThread.SuspendedState getSuspendedState();
  }

  /**
   * A base class for debug element that refers to {@link JavascriptThread.SuspendedState}.
   */
  public static class WithSuspended extends WithSuspendedBase {
    private final JavascriptThread.SuspendedState suspendedState;

    public WithSuspended(JavascriptThread.SuspendedState suspendedState) {
      this.suspendedState = suspendedState;
    }

    @Override
    public SuspendedState getSuspendedState() {
      return suspendedState;
    }
  }

  /**
   * An abstract base class for debug element that refers to {@link EvaluateContext}.
   * It declares no data field.
   */
  public static abstract class WithEvaluateBase extends WithSuspendedBase {
    @Override
    public SuspendedState getSuspendedState() {
      return getEvaluateContext().getThreadSuspendedState();
    }

    public abstract EvaluateContext getEvaluateContext();
  }

  /**
   * A base class for debug element that refers to {@link EvaluateContext}.
   */
  public static class WithEvaluate extends WithEvaluateBase {
    private final EvaluateContext evaluateContext;

    public WithEvaluate(EvaluateContext evaluateContext) {
      this.evaluateContext = evaluateContext;
    }

    @Override
    public EvaluateContext getEvaluateContext() {
      return evaluateContext;
    }
  }
}
