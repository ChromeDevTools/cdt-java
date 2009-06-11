// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.efs.ChromiumScriptFileSystem;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * A utility for interaction with the Eclipse workspace.
 */
public class ChromiumDebugPluginUtil {

  public static final String JS_EXTENSION = "js"; //$NON-NLS-1$

  private static final String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$

  private static final String PROJKEY_QUAL = "Chromium"; //$NON-NLS-1$

  private static final String PROJKEY_LOCAL = "project.type"; //$NON-NLS-1$

  private static final String PROJTYPE_JSDEBUG = "js.debug"; //$NON-NLS-1$

  private static final String JS_EXTENSION_SUFFIX = "." + JS_EXTENSION; //$NON-NLS-1$

  private static final String JS_DEBUG_PROJECT_NATURE = "org.chromium.debug.core.jsnature"; //$NON-NLS-1$


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
   * @param projectName to create
   * @return the newly created project, or {@code null} if the creation failed
   */
  public static IProject createEmptyProject(String projectName) {
    URI projectUri = ChromiumScriptFileSystem.getFileStoreUri(
        new Path(null, "/" + projectName)); //$NON-NLS-1$
    IFileStore projectStore;
    try {
      projectStore = EFS.getStore(projectUri);
      if (projectStore.fetchInfo().exists()) {
        projectStore.delete(EFS.NONE, null);
      }
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    IProjectDescription description =
        ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    description.setLocationURI(projectUri);
    description.setNatureIds(new String[] { JS_DEBUG_PROJECT_NATURE });
    try {
      if (project.exists()) {
        project.delete(true, null);
      }

      project.create(description, null);
      project.open(null);

      QualifiedName key = new QualifiedName(PROJKEY_QUAL, PROJKEY_LOCAL);
      project.setPersistentProperty(key, PROJTYPE_JSDEBUG);

      return project;
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }

    return null;
  }

  /**
   * @param projectName to check for existence
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
   * @param project to create a file in
   * @param filename the base file name to create (shall be suffixed in the
   *        event of name clash)
   * @return the result of IFile.getName(), or null if the creation failed.
   */
  public static IFile createFile(IProject project, String filename) {
    filename = new File(filename).getName(); // simple name
    filename = filename.replace('?', '_');
    filename = filename.endsWith(JS_EXTENSION_SUFFIX)
        ? filename.substring(0, filename.length() - JS_EXTENSION_SUFFIX.length())
        : filename;
    String uniqueName = filename;

    // TODO(apavlov): refactor this?
    for (int i = 1; i < 1000; ++i) {
      String filePathname = uniqueName + JS_EXTENSION_SUFFIX;
      IFile file = project.getFile(filePathname);

      if (file.exists()) {
        uniqueName = filename + "_" + i; //$NON-NLS-1$
      } else {
        try {
          file.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$
        } catch (CoreException e) {
          ChromiumDebugPlugin.log(e);
          return null;
        }
        return file;
      }
    }

    // Can we have 1000 same-named files?
    return null;
  }

  /**
   * Writes data into a resource with the given resourceName residing in the
   * source folder of the given project.
   *
   * @param file to set contents for
   * @param data to write into the file
   * @throws CoreException
   */
  public static void writeFile(IFile file, String data) throws CoreException {
    if (file != null && file.exists()) {
      file.setContents(new ByteArrayInputStream(data.getBytes()), IFile.FORCE, null);
      ResourceAttributes resourceAttributes = file.getResourceAttributes();
      resourceAttributes.setReadOnly(true);
      file.setResourceAttributes(resourceAttributes);
    }
  }

  public static boolean isInteger(String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * The container where the script sources should be put.
   *
   * @param project where the launch configuration stores the scripts
   * @return the script source container
   */
  public static IContainer getSourceContainer(IProject project) {
    return project;
  }

  private ChromiumDebugPluginUtil() {
    // not instantiable
  }
}
