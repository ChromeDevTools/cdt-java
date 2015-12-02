// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core;

import org.chromium.debug.core.model.VmResourceId;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ContainerSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;

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

  public static boolean isGoodTargetContainer(ISourceContainer container) {
    return wrapNonVirtualContainer(container) != null;
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
      return new VmResourceId(name, null);
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
    ContainerWrapper wrapper = wrapNonVirtualContainer(container);
    if (wrapper == null) {
      return null;
    }
    return wrapper.lookup(resource);
  }

  private static ContainerWrapper wrapNonVirtualContainer(ISourceContainer container) {
    if (container instanceof ContainerSourceContainer) {
      final ContainerSourceContainer containerSourceContainer =
          (ContainerSourceContainer) container;
      return new ContainerWrapper() {
        @Override
        public String lookup(IFile resource) {
          return lookupInResourceContainer(resource, containerSourceContainer.getContainer());
        }
      };
    } else if (container instanceof WorkspaceSourceContainer) {
      return new ContainerWrapper() {
        @Override
        public String lookup(IFile resource) {
          return lookupInResourceContainer(resource, ResourcesPlugin.getWorkspace().getRoot());
        }
      };
    } else if (container instanceof SourceNameMapperContainer) {
      SourceNameMapperContainer mappingContainer = (SourceNameMapperContainer) container;
      final ContainerWrapper targetContainerWrapper =
          wrapNonVirtualContainer(mappingContainer.getTargetContainer());
      final String prefix = mappingContainer.getPrefix();
      return new ContainerWrapper() {
        @Override
        public String lookup(IFile resource) {
          String subResult = targetContainerWrapper.lookup(resource);
          if (subResult == null) {
            return null;
          }
          return prefix + subResult;
        }
      };
    }

    return null;
  }

  /**
   * Wraps a container. This interface guarantees that original container with all inner containers
   * are supported by our reversed lookup.
   */
  private interface ContainerWrapper {
    String lookup(IFile resource);
  }

  private static String lookupInResourceContainer(IFile resource, IContainer resourceContainer) {
    IPath resourceFullPath = resource.getFullPath();
    IPath containerFullPath = resourceContainer.getFullPath();
    if (!containerFullPath.isPrefixOf(resourceFullPath)) {
      return null;
    }
    int offset = containerFullPath.segmentCount();
    IPath newPath = resourceFullPath.removeFirstSegments(offset);
    return newPath.toPortableString();
  }
}
