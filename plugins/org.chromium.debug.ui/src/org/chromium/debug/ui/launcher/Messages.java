// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.launcher;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the package.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME =
      "org.chromium.debug.ui.launcher.messages"; //$NON-NLS-1$

  public static String ChromiumRemoteTab_BREAKPOINT_GROUP;

  public static String ChromiumRemoteTab_CONNECTION_FROM_LOCALHOST_WARNING;

  public static String ChromiumRemoteTab_CONNECTION_GROUP;

  public static String ChromiumRemoteTab_ShowDebuggerNetworkCommunication;

  public static String ChromiumRemoteTab_PortLabel;

  public static String ChromiumRemoteTab_HostLabel;

  public static String ChromiumRemoteTab_RemoteTabName;

  public static String ChromiumRemoteTab_AUTO_DETECT_CONTAINER_WARNING;

  public static String ChromiumRemoteTab_AUTODETECT;

  public static String ChromiumRemoteTab_AUTODETECT_LINE1;

  public static String ChromiumRemoteTab_AUTODETECT_LINE2;

  public static String ChromiumRemoteTab_EXACT_MATCH;

  public static String ChromiumRemoteTab_EXACT_MATCH_LINE1;

  public static String ChromiumRemoteTab_EXACT_MATCH_LINE2;

  public static String ChromiumRemoteTab_FILE_PATH;

  public static String ChromiumRemoteTab_InvalidPortNumberError;

  public static String ChromiumRemoteTab_LOOKUP_GROUP_TITLE;

  public static String ChromiumRemoteTab_URL;

  public static String DevToolsProtocolDeprecationTab_CONFIGURATION_NAME_SUFFIX;

  public static String DevToolsProtocolDeprecationTab_COPY_LAUNCH_CONFIGURATION;

  public static String DevToolsProtocolDeprecationTab_MAIN_TEXT;

  public static String DevToolsProtocolDeprecationTab_NEW_PROTOCOL;

  public static String DevToolsProtocolDeprecationTab_OLD_PROTOCOL;

  public static String DevToolsProtocolDeprecationTab_PROJECT_SITE;

  public static String DevToolsProtocolDeprecationTab_SEE_ALSO;

  public static String DevToolsProtocolDeprecationTab_TITLE;

  public static String LaunchType_LogConsoleLaunchName;

  public static String ScriptMappingTab_DESCRIPTION;

  public static String ScriptMappingTab_RECOGNIZED_WRAPPING;

  public static String ScriptMappingTab_TAB_NAME;

  public static String ScriptMappingTab_UNRESOLVED_ERROR_MESSAGE;

  public static String ScriptMappingTab_UNRESOVLED;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
