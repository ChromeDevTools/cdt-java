// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import org.chromium.debug.core.model.ResourceManager;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 * A source lookup director implementation that provides a simple participant and
 * accepts instance of virtual project once it is created.
 */
public class ChromiumSourceDirector extends AbstractSourceLookupDirector {
  private volatile ResourceManager resourceManager = null;
  private volatile IProject project = null;

  public void initializeParticipants() {
    ISourceLookupParticipant participant = new AbstractSourceLookupParticipant() {
      public String getSourceName(Object object) throws CoreException {
        Script script = null;
        if (object instanceof Script) {
          script = (Script) object;
        } else if (object instanceof StackFrame) {
          StackFrame jsStackFrame = (StackFrame) object;
          script = jsStackFrame.getCallFrame().getScript();
        }
        if (script == null) {
          return null;
        }

        return script.getName();
      }
    };
    addParticipants(new ISourceLookupParticipant[] { participant } );
  }

  public void initializeVProjectContainers(IProject project, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.project = project;
  }

  ResourceManager getResourceManager() {
    return resourceManager;
  }

  IProject getProject() {
    return project;
  }
}
