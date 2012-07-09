// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.Collections;
import java.util.Set;

import org.chromium.debug.core.model.LineBreakpointAdapter;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetFactory;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Toggle breakpoint target factory for Chromium breakpoints.
 */
public class ChromiumToggleBreakpointTargetFactory implements IToggleBreakpointsTargetFactory {

  private static final String TOGGLE_TARGET_ID =
      "org.chromium.debug.ui.toggleTargetId"; //$NON-NLS-1$
  private static final Set<String> TOGGLE_TARGET_ID_SET = Collections.singleton(TOGGLE_TARGET_ID);

  @Override
  public Set<String> getToggleTargets(IWorkbenchPart part, ISelection selection) {
    if (isApplicable(part, selection)) {
      return Collections.emptySet();
    }
    return TOGGLE_TARGET_ID_SET;
  }

  @Override
  public String getDefaultToggleTarget(IWorkbenchPart part, ISelection selection) {
    if (isApplicable(part, selection)) {
      return null;
    }
    return TOGGLE_TARGET_ID;
  }

  private boolean isApplicable(IWorkbenchPart part, ISelection selection) {
    return LineBreakpointAdapter.ForVirtualProject.getEditorStatic(part) == null ||
        selection instanceof ITextSelection == false;
  }

  @Override
  public IToggleBreakpointsTarget createToggleTarget(String targetID) {
    return new LineBreakpointAdapter.ForVirtualProject();
  }

  @Override
  public String getToggleTargetName(String targetID) {
    return Messages.ChromiumToggleBreakpointTargetFactory_TOGGLE_TARGET_NAME;
  }

  @Override
  public String getToggleTargetDescription(String targetID) {
    return Messages.ChromiumToggleBreakpointTargetFactory_TOGGLE_TARGET_DESCRIPTION;
  }
}
