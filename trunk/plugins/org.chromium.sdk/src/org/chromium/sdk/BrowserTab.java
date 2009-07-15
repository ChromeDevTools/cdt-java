// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;

/**
 * A lightweight abstraction of a remote Browser tab. Clients can use it to
 * communicate with the debugger instance corresponding to the tab.
 */
public interface BrowserTab {

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
   * @return the "parent" Browser instance
   */
  Browser getBrowser();

  /**
   * @return a URL of the corresponding browser tab
   */
  String getUrl();

  /**
   * Attaches to the related tab debugger.
   *
   * @param listener to report the debug events to
   * @return whether the operation succeeded
   */
  boolean attach(DebugEventListener listener);

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
   * @param callback to invoke once the operation result is available
   */
  void getScripts(ScriptsCallback callback);

  /**
   * Sets a breakpoint with the specified parameters.
   *
   * @param type of the breakpoint
   * @param target of the breakpoint, depends on the {@code type} value:
   *        <table border=1>
   *          <tr><td>type value</td><td>target value</td></tr>
   *          <tr><td>FUNCTION</td><td>a function expression</td></tr>
   *          <tr><td>SCRIPT_NAME</td><td>a script name (as reported by getName())</td></tr>
   *          <tr><td>SCRIPT_ID</td><td>a stringified script ID (as reported by getId())</td></tr>
   *        </table>
   * @param line in the script or function. If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param position of the target start within the line. If none, use
   *        {@link Breakpoint#EMPTY_VALUE}
   * @param enabled whether the breakpoint is enabled initially
   * @param condition nullable string with breakpoint condition
   * @param ignoreCount number specifying the amount of breakpoint hits to
   *        ignore. If none, use {@link Breakpoint#EMPTY_VALUE}
   * @param callback to invoke when the evaluation result is ready
   */
  void setBreakpoint(Breakpoint.Type type, String target, int line, int position, boolean enabled,
      String condition, int ignoreCount, BreakpointCallback callback);

}
