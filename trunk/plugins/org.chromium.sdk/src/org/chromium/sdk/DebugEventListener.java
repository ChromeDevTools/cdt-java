// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * This interface is used by the SDK to report debug events for a certain {@link JavascriptVm} to
 * the clients.
 */
public interface DebugEventListener {

  /**
   * Reports the browser JavaScript virtual machine has suspended (on hitting
   * breakpoints or a step end). The {@code context} can be used to access the
   * current backtrace.
   *
   * @param context associated with the current suspended state
   */
  void suspended(DebugContext context);

  /**
   * Reports the browser JavaScript virtual machine has resumed. This can happen
   * asynchronously, due to a user action in the browser (without explicitly
   * resuming the VM through
   * {@link DebugContext#continueVm(org.chromium.sdk.DebugContext.StepAction, int, org.chromium.sdk.DebugContext.ContinueCallback)}).
   */
  void resumed();

  /**
   * Reports the debug connection has terminated and {@link JavascriptVm} has stopped operating.
   * This event is reported always, regardless of which reason causes termination.
   */
  void disconnected();

  /**
   * Reports that a new script has been loaded into a tab.
   *
   * @param newScript loaded into the tab
   */
  void scriptLoaded(Script newScript);

  /**
   * Reports that the script has been collected and is no longer used in VM.
   */
  void scriptCollected(Script script);

  /**
   * Gets {@link VmStatusListener} that is considered a part of {@link DebugEventListener}.
   * The value this method returns may be cached by caller.
   * @return {@link VmStatusListener} or null
   */
  VmStatusListener getVmStatusListener();

  /**
   * A specialized listener for status of remote VM command queue. After we have sent a request and
   * before VM answers, it is considered busy processing this request.
   */
  interface VmStatusListener {
    /**
     * Reports a new status of remote VM.
     * @param currentRequest name of the oldest request that hasn't been answered yet or null
     * @param numberOfEnqueued number of requests that are expected to be waiting in VM queue; does
     *   not make sense if currentRequest is null
     */
    void busyStatusChanged(String currentRequest, int numberOfEnqueued);
  }

  /**
   * Reports that script source has been altered in remote VM.
   */
  void scriptContentChanged(Script newScript);
}
