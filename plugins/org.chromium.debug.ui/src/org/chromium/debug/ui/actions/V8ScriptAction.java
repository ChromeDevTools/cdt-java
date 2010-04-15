// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A base class for all LiveEdit actions that are scoped to a working file from user workspace.
 * It makes all necessary checks and prepares data in form of {@link FilePair} class.
 * The concrete actions implement the {@link #execute(FilePair)} method.
 */
abstract class V8ScriptAction implements IObjectActionDelegate, IActionDelegate2 {
  private Runnable currentRunnable = null;

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
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
    if (firstElement instanceof IFile == false) {
      return null;
    }
    final IFile file = (IFile) firstElement;
    URI locationURI = file.getLocationURI();
    if (locationURI == null) {
      return null;
    }
    String path = locationURI.getPath();
    if (path == null) {
      return null;
    }
    if (!path.endsWith(".js") && !path.endsWith(".chromium")) { //$NON-NLS-1$ //$NON-NLS-2$
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
    List<? extends FilePair> filePairList = getFilePairs(file);
    FilePair filePair = getSingleFilePair(filePairList);
    execute(filePair);
  }

  protected abstract void execute(FilePair filePair);

  /**
   * A temporary method that excludes all cases when there are more than one file pair for a
   * user file. The proper solution ought to provide a UI for user so that he could review
   * which debug sessions should be included in action.
   */
  private static FilePair getSingleFilePair(List<? extends FilePair> pairs) {
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
   * Finds all file pairs for a user working file. One working file may correspond to several
   * scripts if there are more than one debug sessions.
   */
  private static List<? extends FilePair> getFilePairs(IFile localFile) {
    List<DebugTargetImpl> targetList = DebugTargetImpl.getAllDebugTargetImpls();
    ArrayList<FilePair> result = new ArrayList<FilePair>(targetList.size());

    for (DebugTargetImpl target : targetList) {
      Script script = target.getScript(localFile);
      if (script == null) {
        continue;
      }
      result.add(new FilePair(localFile, script, target));
    }
    return result;
  }

  protected static class FilePair {
    private final IFile file;
    private final Script script;
    private final DebugTargetImpl debugTargetImpl;

    FilePair(IFile file, Script script, DebugTargetImpl debugTargetImpl) {
      this.file = file;
      this.script = script;
      this.debugTargetImpl = debugTargetImpl;
    }
    protected IFile getFile() {
      return file;
    }
    protected Script getScript() {
      return script;
    }
    protected JavascriptVm getJavascriptVm() {
      return debugTargetImpl.getJavascriptEmbedder().getJavascriptVm();
    }
    protected DebugTargetImpl getDebugTarget() {
      return debugTargetImpl;
    }
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

