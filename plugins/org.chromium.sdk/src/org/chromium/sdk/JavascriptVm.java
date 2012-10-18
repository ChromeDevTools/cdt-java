// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;

import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * Abstraction of a remote JavaScript virtual machine. Clients can use it to
 * conduct debugging process.
 * @see Browser.TabConnector#attach
 * @see BrowserFactory#createStandalone
 * @see org.chromium.sdk.wip.WipBrowser.WipTabConnector#attach(TabDebugEventListener)
 */
public interface JavascriptVm {

  /**
   * A callback for breakpoint-related requests.
   */
  interface BreakpointCallback {

    void success(Breakpoint breakpoint);

    void failure(String errorMessage);
  }

  /**
   * A callback for retrieving scripts.
   */
  public interface ScriptsCallback {

    /**
     * This method provides a synchronous access to script collection. All script events
     * are postponed for the time of this call.
     */
    void success(Collection<Script> scripts);

    void failure(String errorMessage);
  }

  /**
   * A callback for suspend request.
   */
  public interface SuspendCallback {

    /**
     * Signals that command successfully finished. After this DebugContext should be built
     * and unless there are some problems,
     * {@link DebugEventListener#suspended(DebugContext)} will be called soon.
     */
    void success();

    void failure(Exception reason);
  }

  /**
   * Detaches from the related tab debugger.
   *
   * @return whether the operation succeeded
   */
  boolean detach();

  /**
   * @return whether the tab is currently attached
   */
  boolean isAttached();

  /**
   * Returns user scripts loaded into the tab. May block until scripts are actually loaded.
   * <p>Some scripts could be already collected when the method is called. They can be missed
   * in the result list.
   * @param callback that synchronously receives result, may be {@code null}
   * @throws MethodIsBlockingException because it may need to actually load scripts
   */
  // TODO: support notification about collected scripts in all backends.
  void getScripts(ScriptsCallback callback) throws MethodIsBlockingException;

  /**
   * Sets a breakpoint with the specified parameters.
   * @param target of the breakpoint
   * @param line in the script or function (0-based). If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param column of the target start within the line (0-based). If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param enabled whether the breakpoint is enabled initially
   * @param condition nullable string with breakpoint condition
   * @param callback to invoke when the evaluation result is ready,
   *        may be {@code null}
   */
  RelayOk setBreakpoint(Breakpoint.Target target, int line, int column, boolean enabled,
      String condition, BreakpointCallback callback, SyncCallback syncCallback);

  /**
   * Tries to suspend VM. If successful, {@link DebugEventListener#suspended(DebugContext)}
   * will be called.
   * @param callback to invoke once the operation result is available,
   *        may be {@code null}
   */
  void suspend(SuspendCallback callback);

  interface ListBreakpointsCallback {
    void success(Collection<? extends Breakpoint> breakpoints);
    void failure(Exception exception);
  }

  /**
   * Asynchronously reads breakpoints from remote VM. The now-effective collection of breakpoints
   * is returned to callback. Already existing {@link Breakpoint} instances are preserved.
   */
  RelayOk listBreakpoints(ListBreakpointsCallback callback, SyncCallback syncCallback);

  /**
   * Asynchronously enables or disables all breakpoints on remote. 'Enabled' means that
   * breakpoints behave as normal, 'disabled' means that VM doesn't stop on breakpoints.
   * It doesn't update individual properties of {@link Breakpoint}s. Method call
   * with a null value and not null callback simply returns current value.
   * @param enabled new value to set or null
   * @param callback receives current value if succeed or error message
   */
  RelayOk enableBreakpoints(Boolean enabled, GenericCallback<Boolean> callback,
      SyncCallback syncCallback);

  /**
   * Defines when VM will break on exception throw (before stack unwind happened).
   */
  enum ExceptionCatchMode {
    /**
     * VM always breaks when exception is being thrown.
     */
    ALL,

    /**
     * VM breaks when exception is being thrown without try-catch that is going to catch it.
     */
    UNCAUGHT,

    /**
     * VM doesn't break when exception is being thrown.
     */
    NONE
  }

  /**
   * Controls whether VM stops on exceptions. 3 catch modes are supported.
   * @param catchMode new mode or null to keep the current mode
   * @param callback gets invoked when operation is finished and receives current mode
   *     as a value (may receive null if actual mode doesn't fit into {@link ExceptionCatchMode}
   *     type)
   */
  RelayOk setBreakOnException(ExceptionCatchMode catchMode,
      GenericCallback<ExceptionCatchMode> callback, SyncCallback syncCallback);

  /**
   * @return version of JavaScript VM or null if not available
   */
  Version getVersion();

  /**
   * @return extension to standard breakpoint target types
   */
  BreakpointTypeExtension getBreakpointTypeExtension();

  /**
   * @return extension that supports ignore count property of breakpoint
   *     or null if unsupported by VM
   */
  IgnoreCountBreakpointExtension getIgnoreCountBreakpointExtension();

  /**
   * @return extension that returns function hidden scopes or null if unsupported by VM
   */
  FunctionScopeExtension getFunctionScopeExtension();

  /**
   * @return extension that restarts frame or null if unsupported by VM
   */
  RestartFrameExtension getRestartFrameExtension();
}
