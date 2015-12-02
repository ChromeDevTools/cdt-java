// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
