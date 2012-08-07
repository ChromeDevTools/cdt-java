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

  public static String ChromiumExceptionBreakpoint_MessageMarkerFormat;

  public static String ChromiumTabSelectionDialog_DialogTitle;

  public static String ChromiumTabSelectionDialog_IdColumnName;

  public static String ChromiumTabSelectionDialog_TableTitle;

  public static String ChromiumTabSelectionDialog_UrlColumnName;

  public static String ConnectionLoggerImpl_MessageSeparator;

  public static String ConnectionLoggerImpl_ReceivedFromChrome;

  public static String ConnectionLoggerImpl_SentToChrome;

  public static String DebugTargetImpl_BadResultWhileDisconnecting;

  public static String DebugTargetImpl_BUSY_WITH;

  public static String DebugTargetImpl_CannotStartMultipleDebuggers;

  public static String DebugTargetImpl_Caught;

  public static String DebugTargetImpl_FailedToStartSocketConnection;

  public static String DebugTargetImpl_LogExceptionFormat;

  public static String DebugTargetImpl_TARGET_NAME_PATTERN;

  public static String DebugTargetImpl_TargetName;

  public static String DebugTargetImpl_Uncaught;

  public static String DebugTargetImpl_Unknown;

  public static String HardcodedSourceWrapProvider_DESCRIPTION;

  public static String HardcodedSourceWrapProvider_WITH_DEFINED;

  public static String HardcodedSourceWrapProvider_STANDARD;

  public static String HardcodedSourceWrapProvider_STANDARD_2;

  public static String HardcodedSourceWrapProvider_WITH_DEFINED_2;

  public static String JavascriptVmEmbedderFactory_TargetName0;

  public static String JavascriptVmEmbedderFactory_Terminated;

  public static String JavascriptVmEmbedderFactory_TerminatedWithReason;

  public static String JsLineBreakpoint_MessageMarkerFormat;

  public static String JsThread_ThreadLabelFormat;

  public static String JsThread_ThreadLabelRunning;

  public static String JsThread_ThreadLabelSuspended;

  public static String JsThread_ThreadLabelSuspendedExceptionFormat;

  public static String LaunchInitializationProcedure_JOB_NAME;

  public static String LaunchInitializationProcedure_LOAD_SCRIPTS;

  public static String LaunchInitializationProcedure_SET_OPTIONS;

  public static String LaunchInitializationProcedure_SYNCHRONIZE_BREAKPOINTS;

  public static String LaunchInitializationProcedure_UPDATE_DEBUGGER_STATE;

  public static String LaunchParams_MERGE_OPTION;

  public static String LaunchParams_NONE_OPTION;

  public static String LaunchParams_RESET_REMOTE_OPTION;

  public static String MockUpResourceWriter_NOT_A_JAVASCRIPT;

  public static String MockUpResourceWriter_SCRIPT_WITHOUT_TEXT;

  public static String MockUpResourceWriter_SCRIPTS_OVERLAPPED;

  public static String ResourceManager_UnnamedScriptName;

  public static String StackFrame_NameFormat;

  public static String StackFrame_UnknownScriptName;

  public static String TargetInitializeState_INITIALIZING;

  public static String TargetInitializeState_TERMINATED;

  public static String Variable_CANNOT_BUILD_EXPRESSION;

  public static String Variable_NotScalarOrObjectFormat;

  public static String Variable_NullTypeForAVariable;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
