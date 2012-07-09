// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;


/**
 * A lightweight abstraction of a remote Browser tab. Each browser tab
 * corresponds to a Javascript Virtual Machine and is_a {code JavascriptVm}.
 */
public interface BrowserTab extends JavascriptVm {

  /**
   * @return the "parent" Browser instance
   */
  Browser getBrowser();

  /**
   * @return a current URL of the corresponding browser tab
   */
  String getUrl();

}
