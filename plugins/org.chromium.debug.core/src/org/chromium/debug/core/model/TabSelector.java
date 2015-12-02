// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
