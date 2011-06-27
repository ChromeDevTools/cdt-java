// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import java.util.ArrayList;

import org.chromium.debug.core.model.ResourceManager;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.model.VmResource;
import org.chromium.debug.core.model.VmResourceId;
import org.chromium.debug.core.model.VmResourceRef;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
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
    ISourceLookupParticipant participant = new AccurateLookupParticipant();
    addParticipants(new ISourceLookupParticipant[] { participant } );
  }

  public VmResourceRef findVmResourceRef(IFile file) throws CoreException {
    VmResourceId vmResourceId = reverseSourceLookup.findVmResource(file);
    if (vmResourceId == null) {
      return null;
    }
    return VmResourceRef.forVmResourceId(vmResourceId);
  }

  private static class AccurateLookupParticipant extends AbstractSourceLookupParticipant {
    public String getSourceName(Object object) throws CoreException {
      return getSourceNameImpl(object);
    }

    @Override
    public Object[] findSourceElements(Object object) throws CoreException {
      Object[] result = super.findSourceElements(object);
      if (result.length > 0) {
        ArrayList<Object> filtered = new ArrayList<Object>(result.length);
        for (Object obj : result) {
          if (obj instanceof VProjectSourceContainer.LookupResult) {
            VProjectSourceContainer.LookupResult vprojectResult =
                (VProjectSourceContainer.LookupResult) obj;
            expandVProjectResult(vprojectResult, object, filtered);
          } else {
            filtered.add(obj);
          }
        }
        result = filtered.toArray();
      }
      return result;
    }
  }

  private static String getSourceNameImpl(Object object) throws CoreException {
    VmResourceId vmResourceId = getVmResourceId(object);
    if (vmResourceId == null) {
      return null;
    }
    String name = vmResourceId.getName();
    if (name == null) {
      // Do not return null, this will cancel look-up.
      // Return empty string. Virtual project container will pass us some data.
      name = "";
    }
    return name;
  }

  private static VmResourceId getVmResourceId(Object object) throws CoreException {
    if (object instanceof Script) {
      Script script = (Script) object;
      return VmResourceId.forScript(script);
    } else if (object instanceof StackFrame) {
      StackFrame jsStackFrame = (StackFrame) object;
      return jsStackFrame.getVmResourceId();
    } else if (object instanceof Breakpoint) {
      Breakpoint breakpoint = (Breakpoint) object;
      return breakpoint.getTarget().accept(BREAKPOINT_RESOURCE_VISITOR);
    } else if (object instanceof VmResourceId) {
      VmResourceId resourceId = (VmResourceId) object;
      return resourceId;
    } else {
      return null;
    }
  }

  /**
   * Virtual project container cannot properly resolve from a sting name. Instead it returns
   * {@link ResourceManager} object that can be processed here, where we have full
   * {@link VmResourceId}.
   */
  private static void expandVProjectResult(VProjectSourceContainer.LookupResult lookupResult,
      Object object, ArrayList<Object> output) throws CoreException {
    VmResourceId resourceId = getVmResourceId(object);
    if (resourceId.getId() != null) {
      VmResource vmResource = lookupResult.getVmResource(resourceId);
      if (vmResource != null) {
        output.add(vmResource.getVProjectFile());
      }
    }
  }

  private static final Breakpoint.Target.Visitor<VmResourceId> BREAKPOINT_RESOURCE_VISITOR =
      new BreakpointTypeExtension.ScriptRegExpSupport.Visitor<VmResourceId>() {
        @Override public VmResourceId visitScriptName(String scriptName) {
          return new VmResourceId(scriptName, null);
        }
        @Override public VmResourceId visitScriptId(long scriptId) {
          return new VmResourceId(null, scriptId);
        }
        @Override public VmResourceId visitRegExp(String regExp) {
          // RegExp cannot be converted into VmResourceId without additional context.
          return null;
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
