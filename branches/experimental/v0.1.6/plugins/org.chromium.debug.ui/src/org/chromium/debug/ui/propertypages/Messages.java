// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.propertypages;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.ui.propertypages.messages"; //$NON-NLS-1$

  public static String JavascriptLineBreakpointPage_BreakpointConditionErrorMessage;

  public static String JavascriptLineBreakpointPage_EnableCondition;

  public static String JavascriptLineBreakpointPage_Enabled;

  public static String JavascriptLineBreakpointPage_IgnoreCount;

  public static String JavascriptLineBreakpointPage_IgnoreCountErrorMessage;

  public static String JsLineBreakpointPage_LineNumberLabel;

  public static String JsLineBreakpointPage_ResourceLabel;

  public static String JsLineBreakpointPage_UnknownLineNumber;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
