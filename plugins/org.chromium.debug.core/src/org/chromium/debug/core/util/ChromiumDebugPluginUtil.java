// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.efs.ChromiumScriptFileSystem;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * A utility for interaction with the Eclipse workspace.
 */
public class ChromiumDebugPluginUtil {

  private static final String CHROMIUM_EXTENSION = "chromium"; //$NON-NLS-1$

  public static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<String>(Arrays.asList(CHROMIUM_EXTENSION, "js", //$NON-NLS-1$
          "html", "htm")); //$NON-NLS-1$ //$NON-NLS-2$

  public static final List<String> SUPPORTED_EXTENSIONS_SUFFIX_LIST;
  static {
    SUPPORTED_EXTENSIONS_SUFFIX_LIST = new ArrayList<String>(SUPPORTED_EXTENSIONS.size());
    for (String extension : SUPPORTED_EXTENSIONS) {
      SUPPORTED_EXTENSIONS_SUFFIX_LIST.add("." + extension); //$NON-NLS-1$
    }
  }

  public static final String JS_DEBUG_PROJECT_NATURE = "org.chromium.debug.core.jsnature"; //$NON-NLS-1$

  public static final String CHROMIUM_EXTENSION_SUFFIX = "." + CHROMIUM_EXTENSION; //$NON-NLS-1$

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
   * Creates an empty workspace project with the name starting with the given projectNameBase.
   * Created project is guaranteed to be new in EFS, but workspace may happen to
   * already have project with such url (left uncleaned from previous runs). Such project
   * silently gets deleted.
   * @param projectNameBase project name template
   * @return the newly created project, or {@code null} if the creation failed
   */
  public static IProject createEmptyProject(String projectNameBase) {
    ProjectCheckData projectProject;
    try {
      for (int uniqueNumber = 0; ; uniqueNumber++) {
        String projectNameTry;
        if (uniqueNumber == 0) {
          projectNameTry = projectNameBase;
        } else {
          projectNameTry = projectNameBase + " (" + uniqueNumber + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        projectProject = checkProjectName(projectNameTry);
        if (projectProject != null) {
          break;
        }
      }
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
    IProject project = projectProject.getProject();
    if (project.exists()) {
      try {
        project.delete(true, null);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(e);
        return null;
      }
    }

    IProjectDescription description =
        ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
    description.setLocationURI(projectProject.getProjectUri());
    description.setNatureIds(new String[] { JS_DEBUG_PROJECT_NATURE });
    try {

      project.create(description, null);
      project.open(null);

      return project;
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
  }

  private interface ProjectCheckData {
    IProject getProject();
    URI getProjectUri();
  }

  /**
   * Checks whether debug virtual project can be created.
   * @param projectNameTry desired project name
   * @return project project with parameters data or null if project with desired name cannot be
   *         created
   */
  private static ProjectCheckData checkProjectName(String projectNameTry) throws CoreException {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectNameTry);
    if (project.exists()) {
      URI projectURI = project.getLocationURI();
      if (!ChromiumScriptFileSystem.isChromiumDebugURI(projectURI)) {
        // This is not our project. Do not touch it.
        return null;
      }
    }
    IPath newPath = project.getFullPath();
    final URI projectUriTry = ChromiumScriptFileSystem.getFileStoreUri(newPath);
    IFileStore projectStore = EFS.getStore(projectUriTry);
    if (projectStore.fetchInfo().exists()) {
      return null;
    }
    return new ProjectCheckData() {
      public IProject getProject() {
        return project;
      }
      public URI getProjectUri() {
        return projectUriTry;
      }
    };
  }


  /**
   * Removes virtual project which was created for debug session. Does its job
   * asynchronously.
   */
  public static void deleteVirtualProjectAsync(final IProject debugProject) {
    Job job = new Job("Remove virtual project") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        URI projectUri = debugProject.getLocationURI();
        try {
          IFileStore projectStore = EFS.getStore(projectUri);
          if (projectStore.fetchInfo().exists()) {
            projectStore.delete(EFS.NONE, null);
          }
          debugProject.delete(true, null);
        } catch (CoreException e) {
          ChromiumDebugPlugin.log(e);
          return new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
              "Failed to delete virtual project");
        }
        return Status.OK_STATUS;
      }
    };

    job.schedule();
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
   * @param container to create the file in
   * @param filename the base short file name to create (will be sanitized for
   *        illegal chars and, in the case of a name clash, suffixed with "(N)")
   * @return the result of IFile.getName(), or {@code null} if the creation
   *         has failed
   */
  public static IFile createFile(IProject container, String filename) {
    return createFile(container, FileContainerHandler.FOR_PROJECT, filename);
  }

  /**
   * Creates an empty file with the given filename in the given project.
   *
   * @param container to create the file in
   * @param filename the base short file name to create (will be sanitized for
   *        illegal chars and, in the case of a name clash, suffixed with "(N)")
   * @return the result of IFile.getName(), or {@code null} if the creation
   *         has failed
   */
  public static IFile createFile(IFolder container, String filename) {
    return createFile(container, FileContainerHandler.FOR_FOLDER, filename);
  }

  private static <C> IFile createFile(final C container, final FileContainerHandler<C> handler,
      String filename) {
    String patchedName = FILE_NAME_BAD_CHARS.matcher(filename).replaceAll("_");
    // In Linux and Mac it should be enough. However, lets check more.
    if (!ResourcesPlugin.getWorkspace().validateName(patchedName, IResource.FILE).isOK()) {
      patchedName = FILE_NAME_BAD_CHARS_FALLBACK.matcher(filename).replaceAll("_");
    }

    UniqueKeyGenerator.Factory<IFile> factory =
        new UniqueKeyGenerator.Factory<IFile>() {
      public IFile tryCreate(String uniqueName) {
        String filePathname = uniqueName + CHROMIUM_EXTENSION_SUFFIX;
        IFile file = handler.getFile(container, filePathname);
        if (file.exists()) {
          return null;
        }
        try {
          file.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$
        } catch (CoreException e) {
          IStatus status = e.getStatus();
          if (status instanceof IResourceStatus) {
            if (status.getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
              // File with different name case already exists.
              // For our case it's the same as "file.exists()".
              return null;
            }
          }
          throw new RuntimeException(e);
        }
        return file;
      }
    };

    // Can we have 1000 same-named files?
    return UniqueKeyGenerator.createUniqueKey(patchedName, 1000, factory);
  }

  private static final Pattern FILE_NAME_BAD_CHARS = Pattern.compile("[/\\x00]");
  private static final Pattern FILE_NAME_BAD_CHARS_FALLBACK = Pattern.compile("[^\\w\\._-]");

  /**
   * Helper class that provides polymorphic access to {@link IProject} and {@link IContainer}.
   * Used for uniform access to "getFile" methods from both interfaces.
   */
  private static abstract class FileContainerHandler<T> {
    abstract IFile getFile(T container, String name);

    static final FileContainerHandler<IProject> FOR_PROJECT = new FileContainerHandler<IProject>() {
      @Override IFile getFile(IProject project, String name) {
        return project.getFile(name);
      }
    };

    static final FileContainerHandler<IFolder> FOR_FOLDER = new FileContainerHandler<IFolder>() {
      @Override IFile getFile(IFolder folder, String name) {
        return folder.getFile(name);
      }
    };
  }

  /**
   * Writes data into a resource with the given resourceName residing in the
   * source folder of the given project. The previous file content is lost.
   * Temporarily resets the "read-only" file attribute if one is present.
   *
   * @param file to set contents for
   * @param data to write into the file
   * @throws CoreException
   */
  public static void writeFile(IFile file, String data) throws CoreException {
    if (file != null && file.exists()) {
      ResourceAttributes resourceAttributes = file.getResourceAttributes();
      if (resourceAttributes.isReadOnly()) {
        resourceAttributes.setReadOnly(false);
        file.setResourceAttributes(resourceAttributes);
      }
      file.setContents(new ByteArrayInputStream(data.getBytes()), IFile.FORCE, null);
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

  public static byte[] readFileContents(IFile file) throws IOException, CoreException {
    InputStream inputStream = file.getContents();
    try {
      return readBytes(inputStream);
    } finally {
      inputStream.close();
    }
  }

  public static byte[] readBytes(InputStream inputStream) throws IOException {
    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] array = new byte[1024];
    while (true) {
      int len = bufferedInputStream.read(array);
      if (len == -1) {
        break;
      }
      output.write(array, 0, len);
    }
    return output.toByteArray();
  }

  public static final Breakpoint.Target.Visitor<String> BREAKPOINT_TARGET_TO_STRING =
      new BreakpointTypeExtension.ScriptRegExpSupport.Visitor<String>() {
        @Override public String visitScriptName(String scriptName) {
          return "script_name=" + scriptName;
        }
        @Override public String visitScriptId(Object scriptId) {
          return "script_id=" + scriptId;
        }
        @Override public String visitRegExp(String regExp) {
          return "RegExp=" + regExp;
        }
        @Override public String visitUnknown(Breakpoint.Target target) {
          return "Unknown target: + " + target;
        }
      };

  public static <T> T throwUnsupported() {
    throw new UnsupportedOperationException();
  }

  private ChromiumDebugPluginUtil() {
    // not instantiable
  }
}
