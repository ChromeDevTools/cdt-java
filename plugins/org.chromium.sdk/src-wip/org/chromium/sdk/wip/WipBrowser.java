// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.wip;

import java.io.IOException;
import java.util.List;

import org.chromium.sdk.Browser;
import org.chromium.sdk.TabDebugEventListener;

/**
 * WIP interface to browser similar to {@link Browser}.
 */
public interface WipBrowser {

  /**
   * @param backend wip implementation
   */
  List<? extends WipTabConnector> getTabs(WipBackend backend) throws IOException;

  interface WipTabConnector {
    String getTitle();

    /**
     * @return tab url that should be shown to user to let him select one tab from list
     */
    String getUrl();

    /**
     * @return true if the tab is already attached at this moment
     */
    boolean isAlreadyAttached();

    /**
     * Attaches to the related tab debugger.
     *
     * @param listener to report the debug events to
     * @return null if operation failed
     */
    WipBrowserTab attach(TabDebugEventListener listener) throws IOException;
  }
}
