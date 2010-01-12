// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;

import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * Abstraction of a remote Javascript virtual machine. Clients can use it to
 * conduct debugging process. This interface does not specify attach method,
 * because it cannot be polymorphous.
 */
public interface JavascriptVm {

  /**
   * A callback for breakpoint-related requests.
   */
  public interface BreakpointCallback {

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
      String condition, int ignoreCount, BreakpointCallback callback);

  /**
   * Tries to suspend VM. If successful, {@link DebugEventListener#suspended(DebugContext)}
   * will be called.
   * @param callback to invoke once the operation result is available,
   *        may be {@code null}
   */
  void suspend(SuspendCallback callback);
}
