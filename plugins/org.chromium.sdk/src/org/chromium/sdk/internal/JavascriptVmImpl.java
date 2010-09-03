// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.io.IOException;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * Base implementation of JavascriptVm.
 */
public abstract class JavascriptVmImpl implements JavascriptVm {

  protected JavascriptVmImpl() {
  }

  public void suspend(SuspendCallback callback) {
    getDebugSession().suspend(callback);
  }

  public void getScripts(ScriptsCallback callback) throws MethodIsBlockingException {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    getDebugSession().getScriptLoader().loadAllScripts(callback, callbackSemaphore);

    boolean res = callbackSemaphore.tryAcquireDefault();
    if (!res) {
      callback.failure("Timeout");
    }
  }

  public void setBreakpoint(Breakpoint.Type type, String target, int line,
      int position, boolean enabled, String condition, int ignoreCount,
      BreakpointCallback callback, SyncCallback syncCallback) {
    getDebugSession().getBreakpointManager()
        .setBreakpoint(type, target, line, position, enabled, condition, ignoreCount, callback,
        syncCallback);
  }

  public void listBreakpoints(final ListBreakpointsCallback callback, SyncCallback syncCallback) {
    getDebugSession().getBreakpointManager().reloadBreakpoints(callback, syncCallback);
  }

  public void enableBreakpoints(boolean enabled, Void callback, SyncCallback syncCallback) {
    getDebugSession().getBreakpointManager().enableBreakpoints(enabled, callback, syncCallback);
  }

  protected abstract DebugSession getDebugSession();

  // TODO(peter.rybin): This message will be obsolete in JavaSE-1.6.
  public static IOException newIOException(String message, Throwable cause) {
    IOException result = new IOException(message);
    result.initCause(cause);
    return result;
  }
}
