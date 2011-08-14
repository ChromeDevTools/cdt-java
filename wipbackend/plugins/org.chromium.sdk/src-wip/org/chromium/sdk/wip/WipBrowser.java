// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

import java.io.IOException;
import java.util.List;

import org.chromium.sdk.Browser;
import org.chromium.sdk.TabDebugEventListener;

/**
 * WIP interface to browser similar to {@link Browser}.
 */
public interface WipBrowser {
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
     * @param backend wip implementation
     * @return null if operation failed
     */
    WipBrowserTab attach(TabDebugEventListener listener) throws IOException;
  }
}
