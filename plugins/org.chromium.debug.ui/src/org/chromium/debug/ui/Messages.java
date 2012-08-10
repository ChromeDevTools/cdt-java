// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.ui.messages"; //$NON-NLS-1$

  public static String ChromiumDebugUIPlugin_Error;

  public static String ChromiumDebugUIPlugin_Info;

  public static String ChromiumDebugUIPlugin_Warning;

  public static String ChromiumToggleBreakpointTargetFactory_TOGGLE_TARGET_DESCRIPTION;

  public static String ChromiumToggleBreakpointTargetFactory_TOGGLE_TARGET_NAME;

  public static String JsDebugModelPresentation_EXCEPTION_LABEL;

  public static String JsDebugModelPresentation_EXCEPTION_LABEL_CAUGHT_ADDENDUM;

  public static String JsWatchExpressionDelegate_BadStackStructureWhileEvaluating;

  public static String JsWatchExpressionDelegate_ErrorEvaluatingExpression;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
