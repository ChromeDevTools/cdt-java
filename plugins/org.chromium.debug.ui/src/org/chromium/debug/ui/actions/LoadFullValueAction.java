// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.RunningTargetData;
import org.chromium.debug.core.model.Value;
import org.chromium.debug.ui.actions.OpenFunctionAction.VariableWrapper;
import org.chromium.sdk.JsValue;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The action for context menu in Variable/Expression views that loads full text
 * of string variable if it was truncated initially.
 */
public abstract class LoadFullValueAction implements IObjectActionDelegate,
    IActionDelegate2 {
  public static class ForVariable extends LoadFullValueAction {
    public ForVariable() {
      super(OpenFunctionAction.VARIABLE_VIEW_ELEMENT_HANDLER);
    }
  }
  public static class ForExpression extends LoadFullValueAction {
    public ForExpression() {
      super(OpenFunctionAction.EXPRESSION_VIEW_ELEMENT_HANDLER);
    }
  }

  private Runnable currentRunnable = null;
  private final OpenFunctionAction.ElementHandler elementHandler;

  protected LoadFullValueAction(OpenFunctionAction.ElementHandler elementHandler) {
    this.elementHandler = elementHandler;
  }

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
    final VariableWrapper wrapper =
        OpenFunctionAction.getElementFromSelection(selection, elementHandler);
    currentRunnable = createRunnable(wrapper);
    action.setEnabled(currentRunnable != null);
  }

  private Runnable createRunnable(final VariableWrapper wrapper) {
    if (wrapper == null) {
      return null;
    }
    final RunningTargetData runningTargetData = wrapper.getRunningTargetData();
    if (runningTargetData == null) {
      return null;
    }
    final Value value = wrapper.getValue();
    if (value == null || !value.isTruncated()) {
      return null;
    }
    return new Runnable() {
      public void run() {
        Value.ReloadValueCallback callback = new Value.ReloadValueCallback() {
          public void done(boolean changed) {
            if (changed) {
              DebugEvent event =
                  new DebugEvent(wrapper.getDebugElement(), DebugEvent.CHANGE, DebugEvent.CONTENT);
              DebugTargetImpl.fireDebugEvent(event);
            }
          }
        };
        value.reloadBiggerValue(callback);
      }
    };
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
