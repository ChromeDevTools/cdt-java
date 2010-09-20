// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;

import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * Abstraction of a remote Javascript virtual machine. Clients can use it to
 * conduct debugging process. This interface does not specify attach method,
 * because it cannot be polymorphic (its signature should be type-specific).
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
   * Retrieves user scripts loaded into the tab.
   * Blocks until the result is ready.
   *
   * @param callback to invoke once the operation result is available,
   *        may be {@code null}
   * @throws MethodIsBlockingException if called from a callback because it
   *         blocks until scripts are received
   * TODO(peter.rybin): get rid of callback (return result explicitly)
   * TODO(peter.rybin): support notification about collected scripts
   */
  void getScripts(ScriptsCallback callback) throws MethodIsBlockingException;

  /**
   * Sets a breakpoint with the specified parameters.
   *
   * @param type of the breakpoint
   * @param target of the breakpoint, depends on the {@code type} value:
   *        <table border=1>
   *          <tr><td>type value</td><td>target value</td></tr>
   *          <tr><td>FUNCTION</td><td>a function expression</td></tr>
   *          <tr><td>SCRIPT_NAME</td><td>a script name (as reported by Script#getName())</td></tr>
   *          <tr><td>SCRIPT_ID</td><td>a stringified script ID (as reported by Script#getId())</td></tr>
   *        </table>
   * @param line in the script or function. If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param position of the target start within the line. If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param enabled whether the breakpoint is enabled initially
   * @param condition nullable string with breakpoint condition
   * @param ignoreCount number specifying the amount of breakpoint hits to
   *        ignore. If none, use {@link Breakpoint#EMPTY_VALUE}
   * @param callback to invoke when the evaluation result is ready,
   *        may be {@code null}
   */
  void setBreakpoint(Breakpoint.Type type, String target, int line, int position, boolean enabled,
      String condition, int ignoreCount, BreakpointCallback callback, SyncCallback syncCallback);

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
  void listBreakpoints(ListBreakpointsCallback callback, SyncCallback syncCallback);

  /**
   * A generic callback used in operations that a remote variable value.
   */
  interface GenericCallback<T> {
    /**
     * Method is called after variable has been successfully updated.
     * @param value holds an actual new value of variable if provided or null
     */
    void success(T value);
    void failure(Exception exception);
  }

  /**
   * Asynchronously enables or disables all breakpoints on remote. Parameter
   * 'enabled' may be null, in this case the remote value is not modified and can be
   * obtained inside the callback.
   */
  void enableBreakpoints(Boolean enabled, GenericCallback<Boolean> callback,
      SyncCallback syncCallback);

  enum ExceptionCatchType {
    CAUGHT, UNCAUGHT
  }

  /**
   * Asynchronously enables or disables breaking on exception. All exception
   * events are split into 2 categories: caught and uncaught. Parameter
   * 'enabled' may be null, in this case the remote value is not modified and can be
   * obtained inside the callback.
   */
  void setBreakOnException(ExceptionCatchType catchType, Boolean enabled,
      GenericCallback<Boolean> callback, SyncCallback syncCallback);
}
