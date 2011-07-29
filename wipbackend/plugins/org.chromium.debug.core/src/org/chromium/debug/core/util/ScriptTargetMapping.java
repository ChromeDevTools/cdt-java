// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

import java.util.Collection;

import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.VmResource;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;

/**
 * Describes a relation between a file in workspace {@link IFile} and a script
 * on remote VM {@link VmResource}. A file may participate in several mappings simultaneously.
 */
public class ScriptTargetMapping {

  /** Original file that is being mapped, may be workspace file or virtual project file. */
  private final IFile file;

  /** Debug session this resource was mapped within. */
  private final ConnectedTargetData connectedTargetData;

  /**
   * Set of VmResource's that the file gets mapped to. Must be non-empty. Several resources
   * are only possible in 'auto-detect' source lookup mode.
   */
  private final Collection<? extends VmResource> vmResources;

  public ScriptTargetMapping(IFile file, Collection<? extends VmResource> vmResources,
      ConnectedTargetData connectedTargetData) {
    this.file = file;
    this.vmResources = vmResources;
    this.connectedTargetData = connectedTargetData;
  }

  public IFile getFile() {
    return file;
  }

  public Collection<? extends VmResource> getVmResources() {
    return vmResources;
  }

  public JavascriptVm getJavascriptVm() {
    return connectedTargetData.getJavascriptVm();
  }

  public ConnectedTargetData getConnectedTargetData() {
    return connectedTargetData;
  }

  /**
   * Utility method that works in the most cases.
   * TODO: re-design program and handle errors nicer than RuntimeException's
   */
  public Script getSingleScript() {
    Collection<? extends VmResource> vmResources = getVmResources();
    if (vmResources.size() != 1) {
      throw new RuntimeException("Several resources found");
    }
    VmResource resource = vmResources.iterator().next();
    if (resource.getMetadata() instanceof VmResource.ScriptHolder == false) {
      throw new RuntimeException("Unsupported type of resource: " + resource.getMetadata());
    }
    VmResource.ScriptHolder scriptHolder = (VmResource.ScriptHolder) resource.getMetadata();
    return scriptHolder.getSingleScript();
  }
}