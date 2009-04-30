// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.chromium.debug.core.model.StackFrame;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Keeps track of the evaluation context (selected StackFrame) in all the
 * workbench parts.
 *
 * Based on the EvaluationContextManager of JDT.
 */
public class JsEvalContextManager implements IWindowListener,
    IDebugContextListener {

  /** Gets set when the debugging context is available */
  private static final String DEBUGGER_ACTIVE =
      ChromiumDebugUIPlugin.PLUGIN_ID + ".debuggerActive"; //$NON-NLS-1$

  private static JsEvalContextManager instance;

  private IWorkbenchWindow activeWindow;

  private Map<IWorkbenchPage, StackFrame> pageToFrame;

  public JsEvalContextManager() {
    DebugUITools.getDebugContextManager().addDebugContextListener(this);
  }

  public static void startup() {
    Runnable r = new Runnable() {
      public void run() {
        if (instance == null) {
          instance = new JsEvalContextManager();
          IWorkbench workbench = PlatformUI.getWorkbench();
          IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
          for (int i = 0; i < windows.length; i++) {
            instance.windowOpened(windows[i]);
          }
          workbench.addWindowListener(instance);
          instance.activeWindow = workbench.getActiveWorkbenchWindow();
        }
      }
    };
    ChromiumDebugUIPlugin.getDisplay().asyncExec(r);
  }

  @Override
  public void windowActivated(IWorkbenchWindow window) {
    activeWindow = window;
  }

  @Override
  public void windowClosed(IWorkbenchWindow window) {

  }

  @Override
  public void windowDeactivated(IWorkbenchWindow window) {
  }

  @Override
  public void windowOpened(IWorkbenchWindow window) {
  }

  @Override
  public void debugContextChanged(DebugContextEvent event) {
    if ((event.getFlags() & DebugContextEvent.ACTIVATED) > 0) {
      IWorkbenchPart part = event.getDebugContextProvider().getPart();
      if (part != null) {
        IWorkbenchPage page = part.getSite().getPage();
        ISelection selection = event.getContext();
        if (selection instanceof IStructuredSelection) {
          IStructuredSelection ss = (IStructuredSelection) selection;
          if (ss.size() == 1) {
            Object element = ss.getFirstElement();
            if (element instanceof IAdaptable) {
              StackFrame frame = (StackFrame)
                  ((IAdaptable) element).getAdapter(StackFrame.class);
              if (frame != null) {
                setContext(page, frame);
                return;
              }
            }
          }
        }
        // debug context lost
        removeContext(page);
      }
    }
  }

  /**
   * Returns the evaluation context for the given part, or <code>null</code> if
   * none. The evaluation context corresponds to the selected stack frame in the
   * following priority order:
   * <ol>
   * <li>stack frame in the same page</li>
   * <li>stack frame in the same window</li>
   * <li>stack frame in active page of other window</li>
   * <li>stack frame in page of other windows</li>
   * </ol>
   *
   * @param part
   *          the part that the evaluation action was invoked from
   * @return the stack frame that supplies an evaluation context, or
   *         <code>null</code> if none
   */
  public static StackFrame getEvaluationContext(IWorkbenchPart part) {
    IWorkbenchPage page = part.getSite().getPage();
    StackFrame frame = getContext(page);
    if (frame == null) {
      return getEvaluationContext(page.getWorkbenchWindow());
    }
    return frame;
  }

  /**
   * Returns the evaluation context for the given window, or <code>null</code>
   * if none. The evaluation context corresponds to the selected stack frame in
   * the following priority order:
   * <ol>
   * <li>stack frame in active page of the window</li>
   * <li>stack frame in another page of the window</li>
   * <li>stack frame in active page of another window</li>
   * <li>stack frame in a page of another window</li>
   * </ol>
   *
   * @param window
   *          the window that the evaluation action was invoked from, or
   *          <code>null</code> if the current window should be consulted
   * @return the stack frame that supplies an evaluation context, or
   *         <code>null</code> if none
   * @return IJavaStackFrame
   */
  public static StackFrame getEvaluationContext(IWorkbenchWindow window) {
    Set<IWorkbenchWindow> alreadyVisited = new HashSet<IWorkbenchWindow>();
    if (window == null) {
      window = instance.activeWindow;
    }
    return getEvaluationContext(window, alreadyVisited);
  }

  private static StackFrame getEvaluationContext(IWorkbenchWindow window,
      Set<IWorkbenchWindow> alreadyVisited) {
    IWorkbenchPage activePage = window.getActivePage();
    StackFrame frame = null;
    if (activePage != null) {
      frame = getContext(activePage);
    }
    if (frame == null) {
      IWorkbenchPage[] pages = window.getPages();
      for (int i = 0; i < pages.length; i++) {
        if (activePage != pages[i]) {
          frame = getContext(pages[i]);
          if (frame != null) {
            return frame;
          }
        }
      }

      alreadyVisited.add(window);

      IWorkbenchWindow[] windows =
          PlatformUI.getWorkbench().getWorkbenchWindows();
      for (int i = 0; i < windows.length; i++) {
        if (!alreadyVisited.contains(windows[i])) {
          frame = getEvaluationContext(windows[i], alreadyVisited);
          if (frame != null) {
            return frame;
          }
        }
      }
      return null;
    }
    return frame;
  }

  private void removeContext(IWorkbenchPage page) {
    if (pageToFrame != null) {
      pageToFrame.remove(page);
      if (pageToFrame.isEmpty()) {
        System.setProperty(DEBUGGER_ACTIVE, Boolean.FALSE.toString());
      }
    }
  }

  private static StackFrame getContext(IWorkbenchPage page) {
    if (instance != null && instance.pageToFrame != null) {
      return instance.pageToFrame.get(page);
    }
    return null;
  }

  private void setContext(IWorkbenchPage page, StackFrame frame) {
    if (pageToFrame == null) {
      pageToFrame = new HashMap<IWorkbenchPage, StackFrame>();
    }
    pageToFrame.put(page, frame);
    System.setProperty(DEBUGGER_ACTIVE, Boolean.TRUE.toString());
  }

}
