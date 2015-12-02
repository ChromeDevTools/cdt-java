// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * This class provides generic plugin-wide services.
 */
public class PluginUtil {

  private static final String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$

  /**
   * Brings up the "Project Explorer" view in the active workbench window.
   */
  public static void openProjectExplorerView() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null) {
          if (workbench.getWorkbenchWindowCount() == 1) {
            window = workbench.getWorkbenchWindows()[0];
          }
        }
        if (window != null) {
          try {
            window.getActivePage().showView(PROJECT_EXPLORER_ID);
          } catch (PartInitException e) {
            // ignore
          }
        }
      }
    });
  }

  /**
   * Determines whether {@code file} is a .chromium file in a
   * JavaScript debug project.
   *
   * @param file to test
   * @return whether the file extension is ".chromium" and its project has the
   *         ChromiumDebugPluginUtil#JS_DEBUG_PROJECT_NATURE nature
   */
  public static boolean isChromiumDebugFile(IFile file) {
    IProject project = file.getProject();
    try {
      return (project.hasNature(ChromiumDebugPluginUtil.JS_DEBUG_PROJECT_NATURE) &&
          file.getName().endsWith(ChromiumDebugPluginUtil.CHROMIUM_EXTENSION_SUFFIX));
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
      return false;
    }
  }


  /**
   * Removes the ".chromium" extension from the fileName.
   *
   * @param fileName to remove the extension from
   * @param check if true additionally check that fileName actually has ".chromium" extension
   * @return a file name without the ".chromium" extension
   */
  public static String stripChromiumExtension(String fileName, boolean check) {
    if (check && !fileName.endsWith(ChromiumDebugPluginUtil.CHROMIUM_EXTENSION_SUFFIX)) {
      return fileName;
    }
    return fileName.substring(
        0, fileName.length() - ChromiumDebugPluginUtil.CHROMIUM_EXTENSION_SUFFIX.length());
  }

  /**
   * @param font or null to use default font
   */
  public static FontMetrics getFontMetrics(Drawable drawable, Font font) {
    GC gc = new GC(drawable);
    try {
      if (font != null) {
        gc.setFont(font);
      }
      return gc.getFontMetrics();
    } finally {
      gc.dispose();
    }
  }

  private PluginUtil() {
    // not instantiable
  }
}
