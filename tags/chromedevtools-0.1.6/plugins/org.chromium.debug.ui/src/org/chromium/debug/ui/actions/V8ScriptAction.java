// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A base class for all LiveEdit actions that are scoped to a working file from user workspace.
 * It makes all necessary checks and prepares data in form of {@link ScriptTargetMapping} class.
 * The concrete actions implement the {@link #execute(List, Shell)} method.
 */
public abstract class V8ScriptAction implements IObjectActionDelegate, IActionDelegate2 {

  private Runnable currentRunnable = null;
  private Shell currentShell = null;

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    currentShell = targetPart.getSite().getShell();
  }

  public void run(IAction action) {
    if (currentRunnable == null) {
      return;
    }
    currentRunnable.run();
    currentRunnable = null;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    currentRunnable = createRunnable(selection);
    action.setEnabled(currentRunnable != null);
  }

  private Runnable createRunnable(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structured = (IStructuredSelection) selection;
    if (structured.size() != 1) {
      return null;
    }

    Object firstElement = structured.getFirstElement();
    if (firstElement instanceof ResourceMapping == false) {
      return null;
    }
    ResourceMapping resourceMapping = (ResourceMapping) firstElement;
    final List<IResource> resourceList = new ArrayList<IResource>(1);
    IResourceVisitor visitor = new IResourceVisitor() {
      public boolean visit(IResource resource) throws CoreException {
        resourceList.add(resource);
        return false;
      }
    };
    try {
      resourceMapping.accept(null, visitor, null);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
    if (resourceList.size() != 1) {
      return null;
    }
    if (resourceList.get(0) instanceof IFile == false) {
      return null;
    }
    final IFile file = (IFile) resourceList.get(0);
    if (!filterFileName(file.getName())) {
      return null;
    }
    return new Runnable() {
      public void run() {
        try {
          execute(file);
        } catch (RuntimeException e) {
          // TODO(peter.rybin): Handle it.
          throw e;
        }
      }
    };
  }

  private void execute(IFile file) {
    List<? extends ScriptTargetMapping> filePairList =
        ChromiumDebugPlugin.getScriptTargetMapping(file);
    execute(filePairList, currentShell);
  }

  protected abstract void execute(List<? extends ScriptTargetMapping> filePairList, Shell shell);

  /**
   * A temporary method that excludes all cases when there are more than one file pair for a
   * user file. The proper solution ought to provide a UI for user so that he could review
   * which debug sessions should be included in action.
   */
  protected static ScriptTargetMapping getSingleFilePair(
      List<? extends ScriptTargetMapping> pairs) {
    if (pairs.size() == 0) {
      throw new RuntimeException("File is not associated with any V8 VM");
    }
    if (pairs.size() != 1) {
      throw new RuntimeException(
          "File is associated with several V8 VMs, this is not supported yet.");
    }
    return pairs.get(0);
  }

  /**
   * @return true if action should be enabled for this file name
   */
  private boolean filterFileName(String name) {
    for (String suffix : ChromiumDebugPluginUtil.SUPPORTED_EXTENSIONS_SUFFIX_LIST) {
      if (name.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  public void dispose() {
    currentRunnable = null;
  }

  public void init(IAction action) {
  }

  public void runWithEvent(IAction action, Event event) {
    run(action);
  }
}

