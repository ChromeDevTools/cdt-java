// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.io.IOException;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.FunctionScopeExtension;
import org.chromium.sdk.IgnoreCountBreakpointExtension;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.Version;
import org.chromium.sdk.internal.v8native.value.JsFunctionImpl;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * Base implementation of JavascriptVm.
 */
public abstract class JavascriptVmImpl implements JavascriptVm {

  protected JavascriptVmImpl() {
  }

  @Override
  public void suspend(SuspendCallback callback) {
    getDebugSession().suspend(callback);
  }

  // TODO: make sure we do not return those scripts that are reported compiled but not loaded yet.
  @Override
  public void getScripts(ScriptsCallback callback) throws MethodIsBlockingException {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    RelayOk relayOk =
        getDebugSession().getScriptManagerProxy().getAllScripts(callback, callbackSemaphore);

    boolean res = callbackSemaphore.tryAcquireDefault(relayOk);
    if (!res) {
      callback.failure("Timeout");
    }
  }

  @Override
  public RelayOk setBreakpoint(Breakpoint.Target target, int line,
      int column, boolean enabled, String condition,
      BreakpointCallback callback, SyncCallback syncCallback) {
    return getDebugSession().getBreakpointManager()
        .setBreakpoint(target, line, column, enabled, condition, callback, syncCallback);
  }

  @Override
  public RelayOk listBreakpoints(final ListBreakpointsCallback callback,
      SyncCallback syncCallback) {
    return getDebugSession().getBreakpointManager().reloadBreakpoints(callback, syncCallback);
  }

  @Override
  public RelayOk enableBreakpoints(Boolean enabled, GenericCallback<Boolean> callback,
      SyncCallback syncCallback) {
    return getDebugSession().getBreakpointManager().enableBreakpoints(enabled,
        callback, syncCallback);
  }

  @Override
  public RelayOk setBreakOnException(ExceptionCatchMode catchMode,
      GenericCallback<ExceptionCatchMode> callback, SyncCallback syncCallback) {
    return getDebugSession().getBreakpointManager().setBreakOnException(catchMode,
        callback, syncCallback);
  }

  @Override
  public Version getVersion() {
    return getDebugSession().getVmVersion();
  }

  @Override
  public BreakpointTypeExtension getBreakpointTypeExtension() {
    return getDebugSession().getBreakpointManager().getBreakpointTypeExtension();
  }

  @Override
  public IgnoreCountBreakpointExtension getIgnoreCountBreakpointExtension() {
    return BreakpointImpl.IGNORE_COUNT_EXTENSION;
  }

  @Override
  public FunctionScopeExtension getFunctionScopeExtension() {
    if (!V8VersionFeatures.isFunctionScopeSupported(getDebugSession().getVmVersion())) {
      return null;
    }
    return JsFunctionImpl.FUNCTION_SCOPE_EXTENSION;
  }

  protected abstract DebugSession getDebugSession();

  // TODO(peter.rybin): This message will be obsolete in JavaSE-1.6.
  public static IOException newIOException(String message, Throwable cause) {
    IOException result = new IOException(message);
    result.initCause(cause);
    return result;
  }
}
