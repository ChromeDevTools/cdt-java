// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.ui.actions.messages"; //$NON-NLS-1$

  public static String ExpressionEvaluator_CannotEvaluateWhenNotSuspended;

  public static String ExpressionEvaluator_ErrorEvaluatingExpression;

  public static String ExpressionEvaluator_ErrorInspectingObject;

  public static String ExpressionEvaluator_EvaluationThreadInterrupted;

  public static String ExpressionEvaluator_SocketError;

  public static String ExpressionEvaluator_UnableToEvaluateExpression;

  public static String JsBreakpointPropertiesRulerAction_ItemLabel;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
