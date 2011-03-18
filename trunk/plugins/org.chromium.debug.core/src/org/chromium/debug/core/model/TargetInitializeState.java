// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.concurrent.atomic.AtomicBoolean;

import org.chromium.debug.core.model.DebugTargetImpl.State;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;

/**
 * Implements 'initialize' state of {@link DebugTargetImpl}. Mostly it's just a stub implementation
 * of methods, but it also has its own state that may be switched to 'terminated' if
 * the target was terminated before initialization ended.
 */
class TargetInitializeState extends State {
  private final DebugTargetImpl debugTargetImpl;

  TargetInitializeState(DebugTargetImpl debugTargetImpl) {
    this.debugTargetImpl = debugTargetImpl;
  }

  private final ITerminate terminateAspect = new ITerminate() {
    private final AtomicBoolean isTerminated = new AtomicBoolean();
    @Override
    public void terminate() throws DebugException {
      boolean updated = isTerminated.compareAndSet(false, true);
      if (updated) {
        DebugTargetImpl.fireDebugEvent(new DebugEvent(debugTargetImpl, DebugEvent.TERMINATE));
      }
    }

    @Override
    public boolean isTerminated() {
      return isTerminated.get();
    }

    @Override
    public boolean canTerminate() {
      return !isTerminated();
    }
  };

  @Override
  ITerminate getTerminate() {
    return terminateAspect;
  }

  @Override
  ISuspendResume getSuspendResume() {
    return STUB_SUSPEND_RESUME;
  }

  @Override
  IDisconnect getDisconnect() {
    return STUB_DISONNECT;
  }

  @Override
  IBreakpointListener getBreakpointListner() {
    return STUB_BREAKPOINT_LISTENER;
  }

  @Override
  IThread[] getThreads() throws DebugException {
    return DebugTargetImpl.EMPTY_THREADS;
  }

  @Override
  String getName() {
    if (terminateAspect.isTerminated()) {
      return Messages.TargetInitializeState_TERMINATED;
    } else {
      return Messages.TargetInitializeState_INITIALIZING;
    }
  }

  @Override
  boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return false;
  }

  @Override
  String getVmStatus() {
    return null;
  }

  @Override
  EvaluateContext getEvaluateContext() {
    return null;
  }

  @Override
  RunningTargetData getRunningTargetDataOrNull() {
    return null;
  }

  private static final IBreakpointListener STUB_BREAKPOINT_LISTENER = new IBreakpointListener() {
    @Override public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    }
    @Override public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    }
    @Override public void breakpointAdded(IBreakpoint breakpoint) {
    }
  };

  private static final ISuspendResume STUB_SUSPEND_RESUME = new ISuspendResume() {
    @Override public void suspend() throws DebugException {
    }
    @Override public void resume() throws DebugException {
    }
    @Override public boolean isSuspended() {
      return false;
    }
    @Override public boolean canSuspend() {
      return false;
    }
    @Override public boolean canResume() {
      return false;
    }
  };

  public static final IDisconnect STUB_DISONNECT = new IDisconnect() {
    @Override public boolean isDisconnected() {
      return false;
    }
    @Override public void disconnect() throws DebugException {
    }
    @Override public boolean canDisconnect() {
      return false;
    }
  };
}
