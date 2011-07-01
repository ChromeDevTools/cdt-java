// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.chromium.debug.core.ScriptNameManipulator.ScriptNamePattern;
import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.ResourceManager;
import org.chromium.debug.core.model.VmResourceRef;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.model.VmResource;
import org.chromium.debug.core.model.VmResourceId;
import org.chromium.debug.core.util.AccuratenessProperty;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 * A source lookup director implementation that provides a simple participant and
 * accepts instance of virtual project once it is created.
 */
public class ChromiumSourceDirector extends AbstractSourceLookupDirector {
  private volatile ResourceManager resourceManager = null;
  private volatile IProject project = null;
  private volatile ReverseSourceLookup reverseSourceLookup = null;
  private volatile ScriptNameManipulator scriptNameManipulator = null;

  public void initializeParticipants() {
    ISourceLookupParticipant participant;
    if (isInaccurateMode()) {
      participant = new InaccurateLookupParticipant();
    } else {
      participant = new AccurateLookupParticipant();
    }
    addParticipants(new ISourceLookupParticipant[] { participant } );
  }

  public VmResourceRef findVmResourceRef(IFile file) throws CoreException {
    if (isInaccurateMode()) {
      {
        // Try inside virtual project.
        VmResourceId resourceId = resourceManager.getResourceId(file);
        if (resourceId != null) {
          return VmResourceRef.forVmResourceId(resourceId);
        }
      }
      IPath path = file.getFullPath();
      int accurateness = AccuratenessProperty.read(file);
      if (accurateness > path.segmentCount()) {
        accurateness = path.segmentCount();
      }
      int offset = path.segmentCount() - accurateness;
      List<String> components = new ArrayList<String>(accurateness);
      for (int i = 0; i < accurateness; i++) {
        components.add(path.segment(i + offset));
      }
      ScriptNamePattern pattern = scriptNameManipulator.createPattern(components);
      return VmResourceRef.forInaccurate(pattern);
    } else {
      VmResourceId vmResourceId = reverseSourceLookup.findVmResource(file);
      if (vmResourceId == null) {
        return null;
      }
      return VmResourceRef.forVmResourceId(vmResourceId);
    }
  }

  private boolean isInaccurateMode() {
    return isInaccurateMode(getLaunchConfiguration());
  }

  public static boolean isInaccurateMode(ILaunchConfiguration launchConfiguration) {
    // TODO: support default value from eclipse variables.
    // TODO: move method closer to LaunchParams.
    try {
      return launchConfiguration.getAttribute(LaunchParams.INACCURATE_SOURCE_LOOKUP,
          false);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isFindDuplicates() {
    if (isInaccurateMode()) {
      return true;
    }
    return super.isFindDuplicates();
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

  private class InaccurateLookupParticipant implements ISourceLookupParticipant {

    public String getSourceName(Object object) throws CoreException {
      return getSourceNameImpl(object);
    }

    @Override
    public Object[] findSourceElements(Object object) throws CoreException {
      ArrayList<Object> result = new ArrayList<Object>();
      ScriptNameManipulator.FilePath scriptName = getParsedScriptFileName(object);
      if (scriptName != null) {
        for (ISourceContainer container : getSourceContainers()) {
          try {
            findSourceElements(container, object, scriptName, result);
          } catch (CoreException e) {
            ChromiumDebugPlugin.log(e);
            continue;
          }
          // If one container returned one file -- that's a single uncompromised result.
          IFile oneFile = getSimpleResult(result);
          if (oneFile != null) {
            return new Object[] { oneFile };
          }
        }
      }
      return result.toArray();
    }

    private void findSourceElements(ISourceContainer container, Object object,
        ScriptNameManipulator.FilePath scriptName, ArrayList<Object> output) throws CoreException {
      Object[] objects = container.findSourceElements(scriptName.getLastComponent());

      if (objects.length == 0) {
        return;
      }

      int outputStartPos = output.size();

      for (Object obj : objects) {
        if (obj instanceof IFile) {
          IFile file = (IFile) obj;
          if (matchFileAccurateness(file, scriptName)) {
            output.add(obj);
          }
        } else if (obj instanceof VProjectSourceContainer.LookupResult) {
          VProjectSourceContainer.LookupResult vprojectResult =
              (VProjectSourceContainer.LookupResult) obj;
          expandVProjectResult(vprojectResult, object, output);
        } else {
          output.add(obj);
        }
      }

      int outputEndPos = output.size();

      if (outputEndPos - outputStartPos > 1) {
        // Put short name last. They cannot be filtered out by our rules, so they may be parasite.
        Collections.sort(output.subList(outputStartPos, outputEndPos), SHORT_NAME_LAST);
      }
    }

    private IFile getSimpleResult(List<Object> objects) {
      if (objects.size() != 1) {
        return null;
      }
      Object oneObject = objects.get(0);
      if (oneObject instanceof IFile == false) {
        return null;
      }
      IFile file = (IFile) oneObject;
      return file;
    }

    private boolean matchFileAccurateness(IFile file, ScriptNameManipulator.FilePath scriptName)
        throws CoreException {
      int accurateness = AccuratenessProperty.read(file);
      if (accurateness > AccuratenessProperty.BASE_VALUE) {
        IPath path = file.getFullPath();
        int pathPos = path.segmentCount() - AccuratenessProperty.BASE_VALUE -1 ;
        Iterator<String> scriptIterator = scriptName.iterator();
        while (accurateness > AccuratenessProperty.BASE_VALUE) {
          if (pathPos < 0 || !scriptIterator.hasNext()) {
            return false;
          }
          String scriptComponent = scriptIterator.next();
          String pathComponent = path.segment(pathPos--);
          if (!scriptComponent.equals(pathComponent)) {
            return false;
          }
          accurateness--;
        }
      }
      return true;
    }


    private ScriptNameManipulator.FilePath getParsedScriptFileName(Object object)
        throws CoreException {
      final String scriptName = getVmResourceId(object).getName();
      if (scriptName == null) {
        return UNKNOWN_NAME;
      }
      return scriptNameManipulator.getFileName(scriptName);
    }

    @Override
    public void init(ISourceLookupDirector director) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void sourceContainersChanged(ISourceLookupDirector director) {
    }
  }

  private static final ScriptNameManipulator.FilePath UNKNOWN_NAME =
      new ScriptNameManipulator.FilePath() {
    @Override
    public String getLastComponent() {
      return "<unknonwn source>";
    }

    @Override
    public Iterator<String> iterator() {
      return Collections.<String>emptyList().iterator();
    }
  };


  private static final Comparator<Object> SHORT_NAME_LAST = new Comparator<Object>() {
    @Override
    public int compare(Object o1, Object o2) {
      int len1 = getPathLength(o1);
      int len2 = getPathLength(o2);
      return len2 - len1;
    }

    private int getPathLength(Object obj) {
      if (obj instanceof IFile == false) {
        return Integer.MIN_VALUE;
      }
      IFile file = (IFile) obj;
      return file.getFullPath().segmentCount();
    }
  };

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

  public void initializeVProjectContainers(IProject project, ResourceManager resourceManager,
      ScriptNameManipulator scriptNameManipulator) {
    this.resourceManager = resourceManager;
    this.project = project;
    this.scriptNameManipulator = scriptNameManipulator;
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
