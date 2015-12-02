// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.ui.actions.messages"; //$NON-NLS-1$

  public static String ChooseVmControl_LAUNCH;

  public static String ChooseVmControl_TARGET;

  public static String ExpressionEvaluator_CannotEvaluateWhenNotSuspended;

  public static String ExpressionEvaluator_ErrorEvaluatingExpression;

  public static String ExpressionEvaluator_ErrorInspectingObject;

  public static String ExpressionEvaluator_EvaluationThreadInterrupted;

  public static String ExpressionEvaluator_SocketError;

  public static String ExpressionEvaluator_UnableToEvaluateExpression;

  public static String JsBreakpointPropertiesRulerAction_ItemLabel;

  public static String SynchronizeBreakpoints_JOB_TITLE;

  public static String TemporarilyFormatSourceAction_DELETE_FORMATTER_ACTION_NAME;

  public static String TemporarilyFormatSourceAction_FORMATTER_SUFFIX;

  public static String TemporarilyFormatSourceAction_NO_FORMATTER_DISABLED_SUFFIX;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
