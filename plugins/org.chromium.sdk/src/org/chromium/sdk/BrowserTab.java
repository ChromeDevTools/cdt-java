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
   * @param target function expression, script identification, or handle decimal
   *        number
   * @param line in the script or function. If not used, pass in
   *        {@link Breakpoint#NO_VALUE}
   * @param position of the target start within the line. If not used, pass in
   *        {@link Breakpoint#NO_VALUE}
   * @param enabled whether the breakpoint is enabled initially
   * @param condition nullable string with breakpoint condition
   * @param ignoreCount number specifying the amount of breakpoint hits to
   *        ignore. If not used, pass in {@link Breakpoint#NO_VALUE}
   * @param callback to invoke when the evaluation result is ready
   */
  void setBreakpoint(Breakpoint.Type type, String target, int line, int position, boolean enabled,
      String condition, int ignoreCount, BreakpointCallback callback);

}
