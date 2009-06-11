// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.core.model.messages"; //$NON-NLS-1$

  public static String ChromiumTabSelectionDialog_DialogTitle;

  public static String ChromiumTabSelectionDialog_IdColumnName;

  public static String ChromiumTabSelectionDialog_TableTitle;

  public static String ChromiumTabSelectionDialog_UrlColumnName;

  public static String DebugTargetImpl_BadResultWhileDisconnecting;

  public static String DebugTargetImpl_CannotStartMultipleDebuggers;

  public static String DebugTargetImpl_FailedToStartSocketConnection;

  public static String DebugTargetImpl_TargetName;

  public static String JsLineBreakpoint_MessageMarkerFormat;

  public static String JsThread_ThreadLabelFormat;

  public static String JsThread_ThreadLabelRunning;

  public static String JsThread_ThreadLabelSuspended;

  public static String StackFrame_NameFormat;

  public static String Variable_NotScalarOrObjectFormat;

  public static String Variable_NullTypeForAVariable;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
