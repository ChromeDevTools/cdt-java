// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions.pinpoint;

import org.chromium.debug.core.model.Value;
import org.chromium.debug.ui.actions.OpenFunctionAction;
import org.chromium.debug.ui.actions.OpenFunctionAction.ElementHandler;
import org.chromium.debug.ui.actions.OpenFunctionAction.VariableWrapper;
import org.chromium.debug.ui.actions.SelectionBasedAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Pin-point value as a global object property action.
 * The class is abstract to work in "Variables" and "Expressions" views (see inner classes below).
 */
public abstract class PinPointValueAction extends SelectionBasedAction.Single<VariableWrapper> {

  public static class ForVariable extends PinPointValueAction {
    public ForVariable() {
      super(OpenFunctionAction.VARIABLE_VIEW_ELEMENT_HANDLER);
    }
  }

  public static class ForExpression extends PinPointValueAction {
    public ForExpression() {
      super(OpenFunctionAction.EXPRESSION_VIEW_ELEMENT_HANDLER);
    }
  }

  private final ElementHandler elementHandler;

  protected PinPointValueAction(ElementHandler elementHandler) {
    super(false);
    this.elementHandler = elementHandler;
  }

  @Override
  protected VariableWrapper castElement(Object element) {
    return elementHandler.castElement(element);
  }

  @Override
  protected ActionRunnable createRunnable(VariableWrapper selectedElement) {
    if (selectedElement == null) {
      return null;
    }
    final Value uiValue = selectedElement.getValue();
    if (uiValue == null) {
      return null;
    }
    return new ActionRunnable() {
      @Override
      public void adjustAction() {
      }

      @Override
      public void run(Shell shell, IWorkbenchPart workbenchPart) {
        DialogImpl dialog = new DialogImpl(shell, uiValue);
        dialog.open();
      }
    };
  }
}
