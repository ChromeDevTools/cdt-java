// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.Value;
import org.chromium.debug.core.model.Variable;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Base implementation for generic actions for Variables and Expressions views.
 * Its inner state is {@link Runnable} value: on selection change runnable is calculated,
 * if it's null action is disabled; if it's not-null action is enabled and can be
 * executed.
 */
public abstract class VariableBasedAction implements IObjectActionDelegate,
    IActionDelegate2 {

  public interface VariableWrapper {
    Variable getVariable();
    Value getValue();
    IDebugElement getDebugElement();
    ConnectedTargetData getConnectedTargetData();
  }

  private final ElementHandler elementHandler;
  private Runnable currentRunnable = null;

  protected VariableBasedAction(ElementHandler elementHandler) {
    this.elementHandler = elementHandler;
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void run(IAction action) {
    if (currentRunnable == null) {
      return;
    }
    currentRunnable.run();
  }

  public void dispose() {
    currentRunnable = null;
  }

  public void init(IAction action) {
  }

  public void runWithEvent(IAction action, Event event) {
    if (currentRunnable == null) {
      return;
    }
    currentRunnable.run();
  }

  public void selectionChanged(IAction action, ISelection selection) {
    VariableWrapper wrapper = getElementFromSelection(selection, elementHandler);
    currentRunnable = createRunnable(wrapper);
    action.setEnabled(currentRunnable != null);
  }

  protected abstract Runnable createRunnable(VariableWrapper wrapper);

  static VariableWrapper getElementFromSelection(ISelection selection,
      ElementHandler elementHandler) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    if (structuredSelection.size() != 1) {
      // We do not support multiple selection.
      return null;
    }
    Object element = structuredSelection.getFirstElement();
    return elementHandler.castElement(element);
  }

  public static abstract class ElementHandler {
    public abstract VariableWrapper castElement(Object element);
  }

  public static final ElementHandler VARIABLE_VIEW_ELEMENT_HANDLER = new ElementHandler() {
    @Override public VariableWrapper castElement(Object element) {
      if (element instanceof Variable == false) {
        return null;
      }
      final Variable variable = (Variable) element;
      return new VariableWrapper() {
        @Override public Value getValue() {
          return variable.getValue().asRealValue();
        }
        @Override public Variable getVariable() {
          return variable;
        }
        @Override public IDebugElement getDebugElement() {
          return variable;
        }
        @Override public ConnectedTargetData getConnectedTargetData() {
          return variable.getConnectedData();
        }
      };
    }
  };

  public static final ElementHandler EXPRESSION_VIEW_ELEMENT_HANDLER = new ElementHandler() {
    @Override
    public VariableWrapper castElement(Object element) {
      if (element instanceof IWatchExpression == false) {
        return null;
      }
      final IWatchExpression watchExpression = (IWatchExpression) element;
      return new VariableWrapper() {
        @Override
        public Value getValue() {
          IValue value = watchExpression.getValue();
          if (value instanceof Value == false) {
            return null;
          }
          Value chromiumValue = (Value) value;
          return chromiumValue;
        }
        @Override public Variable getVariable() {
          return null;
        }
        @Override public IDebugElement getDebugElement() {
          return watchExpression;
        }
        @Override
        public ConnectedTargetData getConnectedTargetData() {
          IDebugTarget debugTarget = watchExpression.getDebugTarget();
          if (debugTarget instanceof DebugTargetImpl == false) {
            return null;
          }
          DebugTargetImpl debugTargetImpl = (DebugTargetImpl) debugTarget;
          return debugTargetImpl.getConnectedOrNull();
        }
      };
    }
  };
}
