// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Delegate for JsBreakpointPropertiesRulerAction
 */
public class JsBreakpointPropertiesRulerActionDelegate extends
    AbstractRulerActionDelegate {

  @Override
  protected IAction createAction(ITextEditor editor,
      IVerticalRulerInfo rulerInfo) {
    return new JsBreakpointPropertiesRulerAction(editor, rulerInfo);
  }

}
