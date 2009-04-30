// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * A utility for interaction with the Eclipse workspace.
 */
public class WorkspaceUtil {

  public static final String JS_EXTENSION = "js"; //$NON-NLS-1$

  private static final String PROJECT_EXPLORER_ID =
      "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$

  // TODO(apavlov): revisit the key constants
  private static final String PROJKEY_QUAL = "Chromium"; //$NON-NLS-1$

  private static final String PROJKEY_LOCAL = "project.type"; //$NON-NLS-1$

  private static final String PROJTYPE_JSDEBUG = "debug.js"; //$NON-NLS-1$

  private static final String JS_EXTENSION_SUFFIX = "." + JS_EXTENSION; //$NON-NLS-1$

  /** Javascript source files reside in this folder */
  public static final String SOURCE_FOLDER = "source"; //$NON-NLS-1$

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
   * Creates an empty workspace project with the given projectName. Deletes an
   * existing project beforehand if necessary, and removes all the default
   * contents of the newly created project.
   *
   * @return the newly created project, or null if the creation failed
   */
  public static IProject createEmptyProject(String projectName) {
    IWorkspace ws = ResourcesPlugin.getWorkspace();
    IProject proj = ws.getRoot().getProject(projectName);

    try {
      if (proj.exists()) {
        proj.delete(true, null);
      }

      proj.create(null);
      proj.open(null);

      QualifiedName key = new QualifiedName(PROJKEY_QUAL, PROJKEY_LOCAL);
      proj.setPersistentProperty(key, PROJTYPE_JSDEBUG);

      IResource[] rsrcs = proj.members();
      for (IResource rsrc : rsrcs) {
        rsrc.delete(true, null);
      }

      IFolder src = proj.getFolder(SOURCE_FOLDER);
      if (!src.exists()) {
        src.create(true, true, null);
      }

      return proj;
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }

    return null;
  }

  /**
   * @return whether the project named projectName exists.
   */
  public static boolean projectExists(String projectName) {
    IWorkspace ws = ResourcesPlugin.getWorkspace();
    IProject proj = ws.getRoot().getProject(projectName);
    return proj.exists();
  }

  /**
   * Creates an empty file with the given filename in the given project.
   *
   * @return the result of IFile.getName(), or null if the creation failed.
   */
  public static String createFile(IProject project, String filename) {
    filename = new File(filename).getName(); // simple name
    filename = filename.replace('?', '_');
    filename =
        filename.endsWith(JS_EXTENSION_SUFFIX)
            ? filename.substring(0, filename.length() - JS_EXTENSION_SUFFIX.length())
            : filename;
    String uniqueName = filename;

    // TODO(apavlov): refactor this
    for (int i = 1; i < 1000; ++i) {
      String filePathname =
          getResourceFullName(uniqueName + JS_EXTENSION_SUFFIX);
      IFile file = project.getFile(filePathname);

      if (file.exists()) {
        uniqueName = filename + "_" + i; //$NON-NLS-1$
      } else {
        try {
          file.create(new ByteArrayInputStream("".getBytes()), //$NON-NLS-1$
              false, null);
        } catch (CoreException e) {
          ChromiumDebugPlugin.log(e);
          return null;
        }
        return file.getName();
      }
    }

    // Could we have 1000 same-named files?
    return null;
  }

  /**
   * Writes data into a resource with the given resourceName residing in the
   * source folder of the given project.
   *
   * @throws CoreException
   */
  public static void writeFile(IProject project, String resourceName,
      String data) throws CoreException {
    String resourceFullName = getResourceFullName(resourceName);
    IFile file = project.getFile(resourceFullName);
    if (file != null && file.exists()) {
      file.setContents(new ByteArrayInputStream(data.getBytes()), IFile.FORCE,
          null);
    }
  }

  private static String getResourceFullName(String resourceName) {
    return SOURCE_FOLDER + "\\" + resourceName; //$NON-NLS-1$
  }

  private WorkspaceUtil() {
    // not instantiable
  }
}
