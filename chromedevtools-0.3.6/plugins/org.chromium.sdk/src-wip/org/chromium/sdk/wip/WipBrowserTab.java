// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

/**
 * An abstraction of a remote Browser tab. Each browser tab contains
 * a Javascript Virtual Machine.
 */
public interface WipBrowserTab {
  /**
   * @return the "parent" Browser instance
   */
  WipBrowser getBrowser();

  /**
   * @return JavaScript VM representation of this tab
   */
  WipJavascriptVm getJavascriptVm();

  /**
   * @return a URL of the corresponding browser tab
   */
  String getUrl();
}
