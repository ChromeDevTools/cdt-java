// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Script;
import org.eclipse.core.resources.IResource;

/**
 * This object handles the mapping between {@link Script}s and their corresponding resources
 * inside Eclipse.
 */
public class ResourceManager {
  private final Map<IResource, Script> resourceToScript = new HashMap<IResource, Script>();
  private final Map<Script, IResource> scriptToResource = new HashMap<Script, IResource>();

  public synchronized void putScript(Script script, IResource resource) {
    resourceToScript.put(resource, script);
    scriptToResource.put(script, resource);
  }

  public synchronized Script getScript(IResource resource) {
    return resourceToScript.get(resource);
  }

  public synchronized IResource getResource(Script script) {
    return scriptToResource.get(script);
  }

  public synchronized boolean scriptHasResource(Script script) {
    return getResource(script) != null;
  }
}
