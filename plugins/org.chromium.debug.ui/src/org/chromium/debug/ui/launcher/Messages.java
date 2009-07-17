// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.ui.launcher.messages"; //$NON-NLS-1$

  public static String ChromiumRemoteTab_PortLabel;

  public static String ChromiumRemoteTab_ProjectNameLabel;

  public static String ChromiumRemoteTab_RemoteTabName;
  
  public static String ChromiumRemoteTab_InvalidPortNumberError;
  
  public static String ChromiumRemoteTab_InvalidProjectNameError;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
