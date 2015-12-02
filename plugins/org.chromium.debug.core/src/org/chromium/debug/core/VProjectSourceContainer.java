// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core;

import org.chromium.debug.core.model.ResourceManager;
import org.chromium.debug.core.model.VmResource;
import org.chromium.debug.core.model.VmResourceId;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceContainerTypeDelegate;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;

/**
 * A source container implementation that wraps V8 virtual project. Currently virtual project
 * has a flat file structure, so the container is accordingly one-level.
 * <p>
 * Unlike other implementation of {@link ISourceContainer} this class initially gets instantiated
 * with no data. In this state it serves as an empty container because actual VM scripts are
 * not available yet. Launch configuration UI will use it in this state more as a symbolic
 * place-holder in sources tab. Later when VM is connected, method
 * {@link #init(ISourceLookupDirector)} will be called and the actual content will be set.
 */
public class VProjectSourceContainer implements ISourceContainer {

  private static final String TYPE_ID =
      "org.chromium.debug.core.VProjectSourceContainer.type"; //$NON-NLS-1$

  private ChromiumSourceDirector chromiumSourceDirector = null;

  VProjectSourceContainer() {
  }

  public void init(ISourceLookupDirector director) {
    if (director instanceof ChromiumSourceDirector) {
      chromiumSourceDirector = (ChromiumSourceDirector) director;
    }
  }

  public void dispose() {
  }

  public Object[] findSourceElements(String name) {
    if (chromiumSourceDirector == null) {
      return new Object[0];
    }
    ResourceManager resourceManager = chromiumSourceDirector.getResourceManager();
    LookupResult lookupResult = new LookupResult(resourceManager);
    return new Object[] { lookupResult };
  }

  public String getName() {
    IProject project = null;
    if (chromiumSourceDirector != null) {
      project = chromiumSourceDirector.getProject();
    }
    if (project == null) {
      return Messages.VProjectSourceContainer_DEFAULT_TYPE_NAME;
    } else {
      return project.getName();
    }
  }

  public ISourceContainer[] getSourceContainers() {
    return null;
  }

  public ISourceContainerType getType() {
    return DebugPlugin.getDefault().getLaunchManager().getSourceContainerType(TYPE_ID);
  }

  public boolean isComposite() {
    return false;
  }

  public VmResourceId findScriptId(IFile resource) {
    if (chromiumSourceDirector == null) {
      throw new IllegalStateException();
    }
    return chromiumSourceDirector.getResourceManager().getResourceId(resource);
  }

  public Object getAdapter(Class adapter) {
    return null;
  }

  /**
   * A type delegate that implements a trivial memento. We do not save any actual data here,
   * because it all should be derived from a current VM launch.
   */
  public static class TypeDelegate implements ISourceContainerTypeDelegate {
    public ISourceContainer createSourceContainer(String memento) throws CoreException {
      return new VProjectSourceContainer();
    }

    public String getMemento(ISourceContainer container) throws CoreException {
      return "VProjectSourceContainer.memento.stub"; //$NON-NLS-1$
    }
  }

  /**
   * Result that {@link VProjectSourceContainer#findSourceElements(String)} returns instead of
   * IFile that other containers normally return. This result is could be converted to file
   * later in {@link ChromiumSourceDirector} where scriptId is available.
   * <p>If source director failed to process result, the back-up strategy could be to do
   * something in our implementation of ISourcePresentation.
   *
   */
  public static class LookupResult {
    private final ResourceManager resourceManager;

    LookupResult(ResourceManager resourceManager) {
      this.resourceManager = resourceManager;
    }

    public VmResource getVmResource(VmResourceId resourceId) {
      return resourceManager.getVmResource(resourceId);
    }
  }
}
