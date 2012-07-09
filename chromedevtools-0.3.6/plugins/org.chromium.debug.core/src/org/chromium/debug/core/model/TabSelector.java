// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;

import org.chromium.sdk.Browser;

/**
 * This interface allows clients to provide various strategies
 * for selecting a Chromium tab to debug.
 */
public interface TabSelector {

  /**
   * @param tabFetcher that is used to download list of tabs; list of tabs
   *        may be reloaded if needed
   * @return a tab to debug, or null if the launch configuration should not
   *         proceed attaching to a Chromium tab
   * @throws IOException if tabFetcher got network problems downloading tabs
   */
  Browser.TabConnector selectTab(Browser.TabFetcher tabFetcher) throws IOException;
}
