// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
