// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import org.chromium.debug.core.model.ResourceManager;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.model.VmResourceId;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
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
  private volatile ReverseSourceLookup reverseSourceLookup = null;


  public void initializeParticipants() {
    ISourceLookupParticipant participant = new AbstractSourceLookupParticipant() {
      public String getSourceName(Object object) throws CoreException {
        if (object instanceof Script) {
          Script script = (Script) object;
          return VmResourceId.forScript(script).getEclipseSourceName();
        } else if (object instanceof StackFrame) {
          StackFrame jsStackFrame = (StackFrame) object;
          VmResourceId vmResourceId = jsStackFrame.getVmResourceId();
          if (vmResourceId == null) {
            return null;
          }
          return vmResourceId.getEclipseSourceName();
        } else if (object instanceof Breakpoint) {
          Breakpoint breakpoint = (Breakpoint) object;
          VmResourceId resourceId = breakpoint.getTarget().accept(BREAKPOINT_RESOURCE_VISITOR);
          return resourceId.getEclipseSourceName();
        } else if (object instanceof VmResourceId) {
          VmResourceId resourceId = (VmResourceId) object;
          return resourceId.getEclipseSourceName();
        } else {
          return null;
        }
      }
    };
    addParticipants(new ISourceLookupParticipant[] { participant } );
  }

  private static final Breakpoint.Target.Visitor<VmResourceId> BREAKPOINT_RESOURCE_VISITOR =
      new Breakpoint.Target.Visitor<VmResourceId>() {
        @Override public VmResourceId visitScriptName(String scriptName) {
          return VmResourceId.forName(scriptName);
        }
        @Override public VmResourceId visitScriptId(long scriptId) {
          return VmResourceId.forId(scriptId);
        }
        @Override public VmResourceId visitUnknown(Breakpoint.Target target) {
          return null;
        }
      };

  public void initializeVProjectContainers(IProject project, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.project = project;
    this.reverseSourceLookup = new ReverseSourceLookup(this);
  }

  public ReverseSourceLookup getReverseSourceLookup() {
    return reverseSourceLookup;
  }

  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  IProject getProject() {
    return project;
  }
}
