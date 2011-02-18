// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ChromiumDebugUIPlugin extends AbstractUIPlugin {

  /** The plug-in ID. */
  public static final String PLUGIN_ID = "org.chromium.debug.ui"; //$NON-NLS-1$

  /** Editor ID for JS files. */
  public static final String EDITOR_ID = PLUGIN_ID + ".editor"; //$NON-NLS-1$

  /** The shared instance. */
  private static ChromiumDebugUIPlugin plugin;

  public ChromiumDebugUIPlugin() {
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance.
   *
   * @return the shared instance
   */
  public static ChromiumDebugUIPlugin getDefault() {
    return plugin;
  }

  /**
   * @return a current display, or the default one if the current one is not
   *         available
   */
  public static Display getDisplay() {
    Display display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  /**
   * @return the active workbench shell, or {@code null} if one is not available
   */
  public static Shell getActiveWorkbenchShell() {
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) {
      return window.getShell();
    }
    return null;
  }

  /**
   * @return the active workbench window
   */
  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    return getDefault().getWorkbench().getActiveWorkbenchWindow();
  }

  /**
   * Creates a status dialog using the given {@code status}.
   *
   * @param status to derive the severity
   */
  public static void statusDialog(IStatus status) {
    switch (status.getSeverity()) {
      case IStatus.ERROR:
        statusDialog(Messages.ChromiumDebugUIPlugin_Error, status);
        break;
      case IStatus.WARNING:
        statusDialog(Messages.ChromiumDebugUIPlugin_Warning, status);
        break;
      case IStatus.INFO:
        statusDialog(Messages.ChromiumDebugUIPlugin_Info, status);
        break;
    }
  }

  /**
   * Creates a status dialog using the given {@code status} and {@code title}.
   *
   * @param title of the dialog
   * @param status to derive the severity
   */
  public static void statusDialog(String title, IStatus status) {
    Shell shell = getActiveWorkbenchWindow().getShell();
    if (shell != null) {
      switch (status.getSeverity()) {
        case IStatus.ERROR:
          ErrorDialog.openError(shell, title, null, status);
          break;
        case IStatus.WARNING:
          MessageDialog.openWarning(shell, title, status.getMessage());
          break;
        case IStatus.INFO:
          MessageDialog.openInformation(shell, title, status.getMessage());
          break;
      }
    }
  }

}
