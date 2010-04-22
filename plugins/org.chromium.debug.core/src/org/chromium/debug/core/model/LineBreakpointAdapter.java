// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Adapter to create breakpoints in JS files.
 */
public abstract class LineBreakpointAdapter implements IToggleBreakpointsTarget {

  public static class ForVirtualProject extends LineBreakpointAdapter {
    @Override
    protected ITextEditor getEditor(IWorkbenchPart part) {
      if (part instanceof ITextEditor) {
        ITextEditor editorPart = (ITextEditor) part;
        IResource resource = (IResource) editorPart.getEditorInput().getAdapter(IResource.class);
        if (resource != null &&
            ChromiumDebugPluginUtil.SUPPORTED_EXTENSIONS.contains(resource.getFileExtension())) {
          return editorPart;
        }
      }
      return null;
    }

    @Override
    protected String getDebugModelId() {
      return VProjectWorkspaceBridge.DEBUG_MODEL_ID;
    }
  }

  public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection)
      throws CoreException {
    ITextEditor textEditor = getEditor(part);
    if (textEditor != null) {
      IResource resource = (IResource) textEditor.getEditorInput().getAdapter(IResource.class);
      ITextSelection textSelection = (ITextSelection) selection;
      int lineNumber = textSelection.getStartLine();
      IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
          getDebugModelId());
      for (int i = 0; i < breakpoints.length; i++) {
        IBreakpoint breakpoint = breakpoints[i];
        if (resource.equals(breakpoint.getMarker().getResource())) {
          if (((ILineBreakpoint) breakpoint).getLineNumber() == lineNumber + 1) {
            // remove
            breakpoint.delete();
            return;
          }
        }
      }

      // Line numbers start with 0 in V8, with 1 in Eclipse.
      ChromiumLineBreakpoint lineBreakpoint = new ChromiumLineBreakpoint(resource, lineNumber + 1,
          getDebugModelId());
      DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(lineBreakpoint);
    }
  }

  public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
    return getEditor(part) != null;
  }

  /**
   * Returns the editor being used to edit a PDA file, associated with the given
   * part, or <code>null</code> if none.
   * @param part workbench part
   * @return the editor being used to edit a PDA file, associated with the given
   *         part, or <code>null</code> if none
   */
  protected abstract ITextEditor getEditor(IWorkbenchPart part);

  protected abstract String getDebugModelId();

  public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection)
      throws CoreException {
    // TODO(apavlov): Implement method breakpoints if feasible.
  }

  public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
    // TODO(apavlov): Implement method breakpoints if feasible.
    return true;
  }

  public void toggleWatchpoints(IWorkbenchPart part, ISelection selection)
      throws CoreException {
    // TODO(apavlov): Implement watchpoints if feasible.
  }

  public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
    // TODO(apavlov): Implement watchpoints if feasible.
    return false;
  }
}
