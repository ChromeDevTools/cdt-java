// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * This interface is used by the SDK to report browser-related debug
 * events for a certain tab to the clients.
 */
public interface TabDebugEventListener {
  /**
   * Every {@code TabDebugEventListener} should aggregate
   * {@code DebugEventListener}.
   */
  DebugEventListener getDebugEventListener();

  /**
   * Reports a navigation event on the target tab.
   *
   * @param newUrl the new URL of the debugged tab
   */
  void navigated(String newUrl);

  /**
   * Reports a closing event on the target tab. All following communications
   * with the associated tab are illegal. This call will be followed by
   * call to {@link DebugEventListener#disconnected()}.
   */
  void closed();
}
