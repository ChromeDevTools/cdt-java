// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import org.chromium.debug.core.model.VmResourceId;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ContainerSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;

/**
 * Eclipse has a standard facility for looking up source file for a debug artifact.
 * LiveEdit feature has an opposite problem: find script in remote VM for a particular js file.
 * This class implements some approach to this problem. An instance of this class corresponds
 * to a particular debug launch.
 */
public class ReverseSourceLookup {
  private final ISourceLookupDirector sourceDirector;

  public ReverseSourceLookup(ISourceLookupDirector sourceDirector) {
    this.sourceDirector = sourceDirector;
  }

  /**
   * Tries to find a corresponding script for a file from a user workspace. The method uses
   * the file name and current source lookup rules to retrieve a resource id, regardless of
   * whether the resource has actually been loaded into the VM (you may want to set a breakpoint
   * on resource before it is actually loaded).
   */
  public VmResourceId findVmResource(IFile sourceFile) throws CoreException {
    for (ISourceContainer container : sourceDirector.getSourceContainers()) {
      VmResourceId scriptName = tryForContainer(sourceFile, container);
      if (scriptName != null) {
        return scriptName;
      }
    }
    return null;
  }

  private VmResourceId tryForContainer(IFile sourceFile, ISourceContainer container)
      throws CoreException {
    if (container.isComposite() && isSupportedCompositeContainer(container)) {
      ISourceContainer[] subContainers = container.getSourceContainers();
      for (ISourceContainer subContainer : subContainers) {
        VmResourceId res = tryForContainer(sourceFile, subContainer);
        if (res != null) {
          return res;
        }
      }
      return null;
    } else if (container instanceof VProjectSourceContainer) {
      VProjectSourceContainer projectSourceContainer = (VProjectSourceContainer) container;
      return projectSourceContainer.findScriptId(sourceFile);
    } else {
      String name = tryForNonVirtualContainer(sourceFile, container);
      if (name == null) {
        return null;
      }
      return VmResourceId.forName(name);
    }
  }

  /**
   * We use {@link ISourceContainer#getSourceContainers()} method to unwrap internal containers.
   * However it doesn't make sense for all composite containers (some of them may return their
   * subdirectories as containers, which is not what we need).
   */
  private boolean isSupportedCompositeContainer(ISourceContainer container) {
    return container instanceof DefaultSourceContainer;
  }

  /**
   * @param container that may not wrap VProjectSourceContainer
   */
  private String tryForNonVirtualContainer(IFile resource, ISourceContainer container) {
    if (container instanceof ContainerSourceContainer) {
      ContainerSourceContainer containerSourceContainer = (ContainerSourceContainer) container;
      IContainer resourceContainer = containerSourceContainer.getContainer();
      if (resourceContainer.getFullPath().isPrefixOf(resource.getFullPath())) {
        String name = resource.getFullPath().makeRelativeTo(
            resourceContainer.getFullPath()).toPortableString();
        return name;
      }
    }

    return null;
  }
}
