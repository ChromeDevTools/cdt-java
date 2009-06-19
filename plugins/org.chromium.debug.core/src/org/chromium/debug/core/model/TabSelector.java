// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.sdk.BrowserTab;

/**
 * This interface allows clients to provide various strategies
 * for selecting a Google Chrome tab to debug.
 */
public interface TabSelector {

  /**
   * @param tabs to choose from
   * @return a tab to debug, or null if the launch configuration should not
   *         proceed attaching to a Google Chrome tab
   */
  BrowserTab selectTab(BrowserTab[] tabs);

}
