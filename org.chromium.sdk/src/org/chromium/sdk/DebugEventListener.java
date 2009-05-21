// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;


/**
 * This interface is used by the SDK to report debug events for a certain tab to
 * the clients.
 */
public interface DebugEventListener {

  /**
   * Reports the browser Javascript virtual machine has suspended (on hitting
   * breakpoints or a step end). The {@code context} can be used to access the
   * current backtrace.
   *
   * @param context associated with the current suspended state
   */
  void suspended(DebugContext context);

  /**
   * Reports the browser Javascript virtual machine has resumed.
   */
  void resumed();

  /**
   * Reports the browser debug connection has terminated.
   */
  void disconnected();

  /**
   * Reports a navigation event on the target tab.
   *
   * @param newUrl the new URL of the debugged tab
   */
  void navigated(String newUrl);

  /**
   * Reports a closing event on the target tab. All following communications
   * with the associated tab are illegal.
   */
  void closed();
}
