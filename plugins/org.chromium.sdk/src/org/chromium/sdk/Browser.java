// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.io.IOException;
import java.util.List;

/**
 * An "entry point" of the SDK. A Browser instance is usually constructed once
 * per a debugged browser instance.
 */
public interface Browser {

  /**
   * Establishes the browser connection and checks for the protocol version
   * supported by the remote, then creates object that downloads list of tabs.
   *
   * @return new instance of TabFetcher that must be dismissed after use to control
   *         connection use
   * @throws IOException if there was a transport layer error
   * @throws UnsupportedVersionException if the SDK protocol version is not
   *         compatible with that supported by the browser
   */
  TabFetcher createTabFetcher() throws IOException, UnsupportedVersionException;


  /**
   * Helps to fetch currently opened browser tabs. It also holds open connection to
   * browser. After instance was used {@code #dismiss} should be called to release
   * connection.
   */
  interface TabFetcher {
    /**
     * Retrieves all browser tabs currently opened. Can only be invoked after the
     * browser connection has been successfully established.
     *
     * @return tabs that can be debugged in the associated Browser instance. An
     *         empty list is returned if no tabs are available.
     * @throws IOException if there was a transport layer failure
     * @throws IllegalStateException if this method is called while another
     *         invocation of the same method is in flight, or the Browser instance
     *         is not connected
     */
    List<? extends TabConnector> getTabs() throws IOException, IllegalStateException;

    /**
     * Should release connection. If no browser tabs is attached at the moment,
     * connection may actually close.
     */
    void dismiss();
  }

  /**
   * Tab list item that is fetched from browser. Actual tab debugger may be
   * attached via this object.
   */
  interface TabConnector {
    /**
     * @return tab url that should be shown to user to let him select one tab from list
     */
    String getUrl();

    /**
     * Attaches to the related tab debugger.
     *
     * @param listener to report the debug events to
     * @return null if operation failed
     */
    BrowserTab attach(TabDebugEventListener listener) throws IOException;
  }
}
