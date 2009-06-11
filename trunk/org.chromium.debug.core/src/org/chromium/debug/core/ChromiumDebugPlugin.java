// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import java.text.MessageFormat;

import org.chromium.debug.core.model.ChromiumBreakpointWBAFactory;
import org.chromium.debug.core.model.ChromiumLineBreakpoint;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class that controls the plug-in life cycle.
 */
public class ChromiumDebugPlugin extends Plugin {

  /** The plug-in ID. */
  public static final String PLUGIN_ID = "org.chromium.debug.core"; //$NON-NLS-1$

  /** The debug model ID. */
  public static final String DEBUG_MODEL_ID = "org.chromium.debug"; //$NON-NLS-1$

  /** The Javascript line breakpoint marker. */
  public static final String BP_MARKER = PLUGIN_ID + ".LineBP"; //$NON-NLS-1$

  /** The shared instance. */
  private static ChromiumDebugPlugin plugin;

  private ChromiumBreakpointWBAFactory breakpointWorkbenchAdapterFactory;

  public ChromiumDebugPlugin() {
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    IAdapterManager manager = Platform.getAdapterManager();
    breakpointWorkbenchAdapterFactory = new ChromiumBreakpointWBAFactory();
    manager.registerAdapters(breakpointWorkbenchAdapterFactory, ChromiumLineBreakpoint.class);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    IAdapterManager manager = Platform.getAdapterManager();
    manager.unregisterAdapters(breakpointWorkbenchAdapterFactory);
    super.stop(context);
  }

  /**
   * @return the shared instance
   */
  public static ChromiumDebugPlugin getDefault() {
    return plugin;
  }

  public static boolean isDebug() {
    ChromiumDebugPlugin thePlugin = getDefault();
    return thePlugin != null && thePlugin.isDebugging();
  }

  public static boolean isTransportDebug() {
    return isDebug() &&
        Boolean.valueOf(Platform.getDebugOption(PLUGIN_ID + "/debug/transport")); //$NON-NLS-1$
  }

  public static boolean isV8DebuggerToolDebug() {
    return isDebug() &&
        Boolean.valueOf(Platform.getDebugOption(PLUGIN_ID + "/debug/v8DebuggerTool")); //$NON-NLS-1$
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void log(Throwable e) {
    if (e instanceof CoreException) {
      log(new Status(IStatus.ERROR, PLUGIN_ID,
          ((CoreException) e).getStatus().getSeverity(), e.getMessage(), e.getCause()));
    } else {
      log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.ChromiumDebugPlugin_InternalError, e));
    }
  }

  public static void logError(String message, Object... arguments) {
    log(new Status(Status.ERROR, PLUGIN_ID, getPossiblyFormattedString(message, arguments)));
  }

  public static void logWarning(String message, Object... arguments) {
    log(new Status(Status.WARNING, PLUGIN_ID, getPossiblyFormattedString(message, arguments)));
  }

  private static String getPossiblyFormattedString(String message, Object... arguments) {
    return arguments.length > 0
        ? MessageFormat.format(message, arguments)
        : message;
  }

}
