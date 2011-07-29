// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.core.messages"; //$NON-NLS-1$

  public static String ChromiumDebugPlugin_InternalError;

  public static String ChromiumSourceDirector_WARNING_TEXT_PATTERN;

  public static String ChromiumSourceDirector_WARNING_TITLE;

  public static String SourceNameMapperContainer_NAME;

  public static String VProjectSourceContainer_DEFAULT_TYPE_NAME;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
