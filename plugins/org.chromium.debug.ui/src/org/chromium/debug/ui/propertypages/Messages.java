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

  public static String AccuratenessControl_LABEL_USE;

  public static String AccuratenessControl_LESS_BUTTON;

  public static String AccuratenessControl_MORE_BUTTON;

  public static String BreakpointTechnicalInfoPage_CHOOSE_LAUNCH;

  public static String BreakpointTechnicalInfoPage_NOT_SET;

  public static String BreakpointTechnicalInfoPage_TARGET;

  public static String JavascriptLineBreakpointPage_BreakpointConditionErrorMessage;

  public static String JavascriptLineBreakpointPage_EnableCondition;

  public static String JavascriptLineBreakpointPage_Enabled;

  public static String JavascriptLineBreakpointPage_IgnoreCount;

  public static String JavascriptLineBreakpointPage_IgnoreCountErrorMessage;

  public static String JsExceptionBreakpointPage_CAUGHT;

  public static String JsExceptionBreakpointPage_UNCAUGHT;

  public static String JsLineBreakpointPage_LineNumberLabel;

  public static String JsLineBreakpointPage_ResourceLabel;

  public static String JsLineBreakpointPage_UnknownLineNumber;

  public static String ScriptFilePage_AUTODETECT_PARAMETER;

  public static String ScriptFilePage_CURRENTLY_LINKED_TO_LABEL;

  public static String ScriptFilePage_MULTIPLE_AUTODETECT_MATCH;

  public static String ScriptFilePage_NAME_IN_VPROJECT;

  public static String ScriptFilePage_SCRIPT_NAME;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
