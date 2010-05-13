// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import org.chromium.debug.core.model.ResourceManager;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ContainerSourceContainer;

/**
 * Eclipse has a standard facility for looking up source file for a debug artifact.
 * LiveEdit feature has an opposite problem: find script in remote VM for a particular js file.
 * This class implements some approach to this problem. An instance of this class corresponds
 * to a particular debug launch.
 */
public class ReverseSourceLookup {
  private final ISourceLookupDirector sourceDirector;
  private final ResourceManager resourceManager;

  public ReverseSourceLookup(ISourceLookupDirector sourceDirector,
      ResourceManager resourceManager) {
    this.sourceDirector = sourceDirector;
    this.resourceManager = resourceManager;
  }

  /**
   * Tries to find a corresponding script for a file from a user workspace.
   */
  public Script findScript(IFile resource) {
    String name = calculateScriptName(resource);
    if (name == null) {
      return null;
    }
    return getScript(name, resourceManager);
  }

  /**
   * Calculates corresponding script name for a file from a user workspace. It checks
   * whether file resides in one of source locations. The actual Script instance may not exist.
   */
  public String calculateScriptName(IFile resource) {
    for (ISourceContainer container : sourceDirector.getSourceContainers()) {
      String scriptName = tryForContainer(resource, container);
      if (scriptName != null) {
        return scriptName;
      }
    }
    return null;
  }

  private String tryForContainer(IFile resource, ISourceContainer container) {
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

  private static Script getScript(String name, ResourceManager resourceManager) {
    IFile resource = resourceManager.getResource(name);
    if (resource == null) {
      return null;
    }
    return resourceManager.getScript(resource);
  }

}
