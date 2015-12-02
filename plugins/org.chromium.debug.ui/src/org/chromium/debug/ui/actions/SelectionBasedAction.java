// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A base class for actions. User implementation is responsible
 * for getting elements from context (i.e. selection) and for performing the action itself.
 * This class implements all methods of {@link IObjectActionDelegate} and {@link IActionDelegate2}.
 * @param <S> user type for data that is read from selection
 */
public abstract class SelectionBasedAction<S> implements IObjectActionDelegate, IActionDelegate2 {

  /**
   * Base class for actions that are enabled only for a single element in selection.
   * User implements {@link #castElement(Object)} method that map raw selection object
   * to a custom type T. Action optionally may allow selections with more than 1 object,
   * but it is required that only a single object is filtered out.
   */
  public static abstract class Single<T> extends SelectionBasedAction<T> {
    private final boolean allowMutipleSelection;

    /**
     * @param allowMutipleSelection if false requires that selection contains exactly 1 element;
     *     if true only requires that selection contains exactly 1 element that gets cast
     *     by {@link #castElement(Object)} method
     */
    protected Single(boolean allowMutipleSelection) {
      this.allowMutipleSelection = allowMutipleSelection;
    }

    @Override
    protected T readSelection(IStructuredSelection selection) {
      if (!allowMutipleSelection && selection.size() != 1) {
        return null;
      }
      T result = null;
      for (Iterator<?> it = selection.iterator(); it.hasNext(); ) {
        Object element = it.next();
        T userElement = castElement(element);
        if (userElement == null) {
          continue;
        }
        if (result != null) {
          return null;
        }
        result = userElement;
      }
      return result;
    }

    /**
     * User-provided method that casts raw selection element to user type T. Method
     * may return null which means that this raw object should be ignored.
     */
    protected abstract T castElement(Object element);
  }

  /**
   * Base class for actions that works with any number of elements in selection.
   */
  static abstract class Multiple<T> extends SelectionBasedAction<List<? extends T>> {
  }

  private ActionRunnable currentRunnable = null;
  private Shell currentShell = null;
  private String originalActionText = null;
  private IWorkbenchPart currentTargetPart = null;
  private IAction action;

  protected abstract S readSelection(IStructuredSelection selection);

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    currentShell = targetPart.getSite().getShell();
    currentTargetPart = targetPart;
  }

  public void run(IAction action) {
    assert action == getAction();
    if (currentRunnable == null) {
      return;
    }
    currentRunnable.run(currentShell, currentTargetPart);
    currentRunnable = null;
  }

  public void selectionChanged(IAction actionParam, ISelection selection) {
    currentRunnable = createRunnableFromRawSelection(selection);
    this.action.setEnabled(currentRunnable != null);
    if (currentRunnable == null) {
      restoreActionText();
    } else {
      currentRunnable.adjustAction();
    }
  }

  private ActionRunnable createRunnableFromRawSelection(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structured = (IStructuredSelection) selection;
    final S selectedElements = readSelection(structured);
    return createRunnable(selectedElements);
  }

  protected abstract ActionRunnable createRunnable(S selectedElements);

  protected interface ActionRunnable {
    void adjustAction();
    void run(Shell shell, IWorkbenchPart workbenchPart);
  }

  protected void modifyActionText(String newText) {
    if (originalActionText == null) {
      originalActionText = action.getText();
    }
    action.setText(newText);
  }
  protected void restoreActionText() {
    if (originalActionText != null) {
      action.setText(originalActionText);
      originalActionText = null;
    }
  }
  protected IAction getAction() {
    return action;
  }

  public void dispose() {
    currentRunnable = null;
  }

  public void init(IAction action) {
    this.action = action;
  }

  public void runWithEvent(IAction action, Event event) {
    run(action);
  }
}
