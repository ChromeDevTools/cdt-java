// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
abstract class SelectionBasedAction<S> implements IObjectActionDelegate, IActionDelegate2 {

  /**
   * Base class for actions that are enabled only for a single element in selection.
   * User implements {@link #castElement(Object)} method that map raw selection object
   * to a custom type T. Action optionally may allow selections with more than 1 object,
   * but it is required that only a single object is filtered out.
   */
  static abstract class Single<T> extends SelectionBasedAction<T> {
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

  private Runnable currentRunnable = null;
  private Shell currentShell = null;

  protected abstract void execute(S selected, Shell shell);

  protected abstract S readSelection(IStructuredSelection selection);

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
    final S selectedElements = readSelection(structured);
    return new Runnable() {
      public void run() {
        // TODO(peter.rybin): put in background!
        try {
          execute(selectedElements, currentShell);
        } catch (RuntimeException e) {
          // TODO(peter.rybin): Handle it.
          throw e;
        }
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
