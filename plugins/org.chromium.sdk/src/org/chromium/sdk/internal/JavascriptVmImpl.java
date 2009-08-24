// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * Base implementation of JavascriptVm.
 */
public abstract class JavascriptVmImpl implements JavascriptVm {

  protected JavascriptVmImpl() {
  }

  public void getScripts(ScriptsCallback callback) throws MethodIsBlockingException {
    getDebugSession().loadAllScripts(callback);
  }

  public void setBreakpoint(Breakpoint.Type type, String target, int line,
      int position, boolean enabled, String condition, int ignoreCount,
      BreakpointCallback callback) {
    getDebugSession().getBreakpointManager()
        .setBreakpoint(type, target, line, position, enabled, condition, ignoreCount, callback);
  }

  public abstract DebugEventListener getDebugEventListener();

  public abstract DebugSessionManager getSessionManager();

  public abstract DebugContextImpl getDebugSession();
}
