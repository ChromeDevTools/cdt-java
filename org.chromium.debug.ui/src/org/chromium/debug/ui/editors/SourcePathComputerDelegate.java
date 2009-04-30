// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import org.chromium.debug.core.util.WorkspaceUtil;
import org.chromium.debug.ui.launcher.LaunchType;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;

/**
 * Computes the default source lookup path for the launch configuration. The
 * default source lookup path is the folder or project containing all Javascript
 * source files. If the folder is not specified, the workspace is searched by
 * default.
 */
public class SourcePathComputerDelegate implements ISourcePathComputerDelegate {

  public ISourceContainer[] computeSourceContainers(
      ILaunchConfiguration config, IProgressMonitor monitor)
      throws CoreException {
    String projName = getProjectName(config);
    IProject proj =
        ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
    IFolder folder = proj.getFolder(WorkspaceUtil.SOURCE_FOLDER);
    ISourceContainer sourceContainer = new FolderSourceContainer(folder, false);

    if (sourceContainer == null) {
      sourceContainer = new WorkspaceSourceContainer();
    }
    return new ISourceContainer[] { sourceContainer };
  }

  private String getProjectName(ILaunchConfiguration config)
      throws CoreException {
    return config.getAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME,
        (String) null);
  }
}