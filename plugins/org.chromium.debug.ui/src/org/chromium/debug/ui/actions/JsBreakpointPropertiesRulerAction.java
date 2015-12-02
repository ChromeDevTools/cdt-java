// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ChromiumLineBreakpoint;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.actions.RulerBreakpointAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to bring up the breakpoint properties dialog.
 */
public class JsBreakpointPropertiesRulerAction extends RulerBreakpointAction implements IUpdate {

  private IBreakpoint breakpoint;

  public JsBreakpointPropertiesRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
    super(editor, rulerInfo);
    setText(Messages.JsBreakpointPropertiesRulerAction_ItemLabel);
  }

  @Override
  public void run() {
    if (getBreakpoint() != null) {
      JsBreakpointPropertiesAction.runAction(getBreakpoint(), getEditor().getEditorSite());
    }
  }

  public void update() {
    breakpoint = getBreakpoint();
    setEnabled(breakpoint != null);
  }

  public static class Delegate extends AbstractRulerActionDelegate {
    @Override
    protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
      return new JsBreakpointPropertiesRulerAction(editor, rulerInfo);
    }

  }
}
