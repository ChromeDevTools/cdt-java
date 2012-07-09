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
import org.chromium.debug.core.model.JavascriptVmEmbedder;
import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.LaunchParams.LookupMode;
import org.chromium.debug.core.model.ResourceManager;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.model.VmResource;
import org.chromium.debug.core.model.VmResourceId;
import org.chromium.debug.core.model.VmResourceRef;
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
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

/**
 * A source lookup director implementation that provides a simple participant and
 * accepts instance of virtual project once it is created.
 */
public class ChromiumSourceDirector extends AbstractSourceLookupDirector {
  private volatile ResourceManager resourceManager = null;
  private volatile IProject project = null;
  private volatile ReverseSourceLookup reverseSourceLookup = null;
  private volatile JavascriptVmEmbedder javascriptVmEmbedder = null;

  /**
   * Contains 'true' if we have already shown the warning about unsupported look-up mode.
   * However this should be reset when user switches from one mode to another.
   */
  private volatile boolean lookupWarningShown = false;

  public void initializeParticipants() {
    ISourceLookupParticipant participant = new LookupParticipant(this);
    addParticipants(new ISourceLookupParticipant[] { participant } );

    // Check mode post factum.
    checkSupportedLookupMode();
  }

  public VmResourceRef findVmResourceRef(IFile file) throws CoreException {
    return getLookupModeHandler().findVmResourceRef(file);
  }

  public static LookupMode readLookupMode(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    String lookupStringValue = launchConfiguration.getAttribute(
        LaunchParams.SOURCE_LOOKUP_MODE, (String) null);
    LookupMode value;
    if (lookupStringValue == null) {
      value = LookupMode.DEFAULT_VALUE;
    } else {
      value = LookupMode.STRING_CONVERTER.decode(lookupStringValue);
    }
    return value;
  }

  private LookupModeHandler getLookupModeHandler() {
    LookupMode mode;
    try {
      mode = readLookupMode(getLaunchConfiguration());
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
      mode = LookupMode.DEFAULT_VALUE;
    }
    return mode.accept(MODE_TO_HANDLER_VISITOR);
  }

  private final LookupMode.Visitor<LookupModeHandler> MODE_TO_HANDLER_VISITOR =
      new LookupMode.Visitor<LookupModeHandler>() {
        @Override
        public LookupModeHandler visitExactMatch() {
          return exactMatchLookupMode;
        }

        @Override
        public LookupModeHandler visitAutoDetect() {
          return autoDetectLookupMode;
        }
      };

  @Override
  public boolean isFindDuplicates() {
    return getLookupModeHandler().forceFindDuplicates() || super.isFindDuplicates();
  }

  /**
   * A single implementation of look participant. This way the participant may decide to become
   * exact match/auto-detect after it is created, because javascriptVm instances comes too late
   * after everything is created.
   */
  private static class LookupParticipant extends AbstractSourceLookupParticipant {
    private final SuperClassAccess superClassAccess = new SuperClassAccess();
    private final ChromiumSourceDirector chromiumSourceDirector;

    LookupParticipant(ChromiumSourceDirector chromiumSourceDirector) {
      this.chromiumSourceDirector = chromiumSourceDirector;
    }

    public String getSourceName(Object object) throws CoreException {
      return getSourceNameImpl(object);
    }

    @Override
    public Object[] findSourceElements(Object object) throws CoreException {
      Delegate delegate = chromiumSourceDirector.getLookupModeHandler().getDelegate();
      return delegate.findSourceElements(object, superClassAccess);
    }

    private Object[] findSourceElementsSuper(Object object) throws CoreException {
      return super.findSourceElements(object);
    }


    static abstract class Delegate {
      abstract Object[] findSourceElements(Object object, SuperClassAccess superClass)
          throws CoreException;
    }

    class SuperClassAccess {
      Object[] findSourceElements(Object object) throws CoreException {
        return findSourceElementsSuper(object);
      }

      ISourceContainer[] getSourceContainers() {
        return LookupParticipant.this.getSourceContainers();
      }

      ChromiumSourceDirector getChromiumSourceDirector() {
        return chromiumSourceDirector;
      }
    }
  }

  private static final LookupParticipant.Delegate EXACT_MATCH_DELEGATE =
      new LookupParticipant.Delegate() {
    @Override
    Object[] findSourceElements(Object object, LookupParticipant.SuperClassAccess superClass)
        throws CoreException {
      Object[] result = superClass.findSourceElements(object);
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
  };

  private static final LookupParticipant.Delegate AUTO_DETECT_DELEGATE =
      new LookupParticipant.Delegate() {
        @Override
        Object[] findSourceElements(Object object, LookupParticipant.SuperClassAccess superClass)
            throws CoreException {
          ArrayList<Object> result = new ArrayList<Object>();
          JavascriptVmEmbedder vmEmbedder =
              superClass.getChromiumSourceDirector().javascriptVmEmbedder;
          ScriptNameManipulator.FilePath scriptName =
              getParsedScriptFileName(object, vmEmbedder.getScriptNameManipulator());
          if (scriptName != null) {
            for (ISourceContainer container : superClass.getSourceContainers()) {
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
            ScriptNameManipulator.FilePath scriptName, ArrayList<Object> output)
            throws CoreException {
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
            // Put short name last. They cannot be filtered out by our rules, so they may
            // be parasite.
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

        private boolean matchFileAccurateness(IFile file,
            ScriptNameManipulator.FilePath scriptName) throws CoreException {
          int accurateness = AccuratenessProperty.read(file);
          if (accurateness > AccuratenessProperty.BASE_VALUE) {
            IPath path = file.getFullPath();
            int pathPos = path.segmentCount() - AccuratenessProperty.BASE_VALUE -1;
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


        private ScriptNameManipulator.FilePath getParsedScriptFileName(Object object,
            ScriptNameManipulator nameManipulator) throws CoreException {
          VmResourceId vmResourceId = getVmResourceId(object);
          if (vmResourceId == null) {
            return null;
          }
          final String scriptName = vmResourceId.getName();
          if (scriptName == null) {
            return UNKNOWN_NAME;
          }
          return nameManipulator.getFileName(scriptName);
        }
      };

  private static String getSourceNameImpl(Object object) throws CoreException {
    VmResourceId vmResourceId = getVmResourceId(object);
    if (vmResourceId == null) {
      return null;
    }
    String name = vmResourceId.getName();
    if (name == null) {
      // Do not return null, this will cancel look-up.
      // Return empty string. Virtual project container will pass us some data.
      name = ""; //$NON-NLS-1$
    }
    return name;
  }

  private static final ScriptNameManipulator.FilePath UNKNOWN_NAME =
      new ScriptNameManipulator.FilePath() {
    @Override
    public String getLastComponent() {
      return "<unknonwn source>"; //$NON-NLS-1$
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
        @Override public VmResourceId visitScriptId(Object scriptId) {
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
      JavascriptVmEmbedder javascriptVmEmbedder) {
    this.resourceManager = resourceManager;
    this.project = project;
    this.javascriptVmEmbedder = javascriptVmEmbedder;
    this.reverseSourceLookup = new ReverseSourceLookup(this);
    checkSupportedLookupMode();
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

  private void checkSupportedLookupMode() {
    LookupModeHandler lookupMode = getLookupModeHandler();
    if (javascriptVmEmbedder != null) {
      lookupMode.showUnsupportedWarning(javascriptVmEmbedder);
    }
  }

  private static abstract class LookupModeHandler {
    abstract LookupParticipant.Delegate getDelegate();

    abstract void showUnsupportedWarning(JavascriptVmEmbedder javascriptVmEmbedder);

    abstract boolean forceFindDuplicates();

    abstract  VmResourceRef findVmResourceRef(IFile file) throws CoreException;
  }

  private final LookupModeHandler exactMatchLookupMode = new LookupModeHandler() {
    @Override LookupParticipant.Delegate getDelegate() {
      return EXACT_MATCH_DELEGATE;
    }

    @Override void showUnsupportedWarning(JavascriptVmEmbedder javascriptVmEmbedder) {
      // 'Exact match' is chosen. Enable warning again.
      lookupWarningShown = false;
    }

    @Override boolean forceFindDuplicates() {
      return false;
    }

    @Override
    VmResourceRef findVmResourceRef(IFile file) throws CoreException {
      VmResourceId vmResourceId = reverseSourceLookup.findVmResource(file);
      if (vmResourceId == null) {
        return null;
      }
      return VmResourceRef.forVmResourceId(vmResourceId);
    }
  };

  private final LookupModeHandler autoDetectLookupMode = new LookupModeHandler() {
    @Override LookupParticipant.Delegate getDelegate() {
      return AUTO_DETECT_DELEGATE;
    }

    @Override
    void showUnsupportedWarning(final JavascriptVmEmbedder javascriptVmEmbedder) {
      if (lookupWarningShown) {
        return;
      }
      BreakpointTypeExtension breakpointTypeExtension =
          javascriptVmEmbedder.getJavascriptVm().getBreakpointTypeExtension();
      BreakpointTypeExtension.ScriptRegExpSupport scriptRegExpSupport =
          breakpointTypeExtension.getScriptRegExpSupport();
      if (scriptRegExpSupport != null) {
        return;
      }
      lookupWarningShown = true;
      Display display = Display.getDefault();
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          Display display = Display.getDefault();
          MessageBox messageBox = new MessageBox(display.getActiveShell(), SWT.ICON_WARNING);
          messageBox.setText(Messages.ChromiumSourceDirector_WARNING_TITLE);
          String messagePattern = Messages.ChromiumSourceDirector_WARNING_TEXT_PATTERN;
          String message = NLS.bind(messagePattern,
              javascriptVmEmbedder.getJavascriptVm().getVersion());
          messageBox.setMessage(message);
          messageBox.open();
        }
      });
    }

    @Override boolean forceFindDuplicates() {
      return true;
    }

    @Override
    VmResourceRef findVmResourceRef(IFile file) throws CoreException {
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
      ScriptNameManipulator scriptNameManipulator = javascriptVmEmbedder.getScriptNameManipulator();
      ScriptNamePattern pattern = scriptNameManipulator.createPattern(components);
      return VmResourceRef.forRegExpBased(pattern);
    }
  };
}
