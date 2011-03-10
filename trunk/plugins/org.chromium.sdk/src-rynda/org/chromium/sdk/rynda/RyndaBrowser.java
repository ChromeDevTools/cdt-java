// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.rynda;

import org.chromium.sdk.Browser;
import org.chromium.sdk.Browser.TabConnector;

/**
 * A temporary interface for WebProtocol-connected (i.e. Rynda protocol-base) browser.
 * It doesn't provide any means of reading browser tab list.
 * This class should be replaced with a regular {@link Browser} interface once
 * we can read a list of tabs from remote.
 */
public interface RyndaBrowser {
  /**
   * @param tabId an internal id of the tab that user should obtain by himself
   */
  TabConnector getTabConnector(int tabId);
}
