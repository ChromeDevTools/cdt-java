// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.chromium.debug.core.model.EvaluateContext;
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
 * workbench parts. A singleton.
 */
public class JsEvalContextManager implements IWindowListener, IDebugContextListener {

  private static final String DEBUGGER_ACTIVE = ChromiumDebugUIPlugin.PLUGIN_ID + ".debuggerActive"; //$NON-NLS-1$

  private static JsEvalContextManager instance;

  private IWorkbenchWindow activeWindow;

  private final Map<IWorkbenchPage, EvaluateContext> pageToFrame =
      new HashMap<IWorkbenchPage, EvaluateContext>();

  protected JsEvalContextManager() {
    DebugUITools.getDebugContextManager().addDebugContextListener(this);
  }

  /**
   * This method will get called only once.
   */
  public static void startup() {
    Runnable r = new Runnable() {
      public void run() {
        if (instance == null) {
          instance = new JsEvalContextManager();
          IWorkbench workbench = PlatformUI.getWorkbench();
          workbench.addWindowListener(instance);
          instance.activeWindow = workbench.getActiveWorkbenchWindow();
        }
      }
    };
    ChromiumDebugUIPlugin.getDisplay().asyncExec(r);
  }

  public void windowActivated(IWorkbenchWindow window) {
    activeWindow = window;
  }

  public void windowClosed(IWorkbenchWindow window) {
  }

  public void windowDeactivated(IWorkbenchWindow window) {
  }

  public void windowOpened(IWorkbenchWindow window) {
  }

  public void debugContextChanged(DebugContextEvent event) {
    if ((event.getFlags() & DebugContextEvent.ACTIVATED) > 0) {
      IWorkbenchPart part = event.getDebugContextProvider().getPart();
      if (part == null) {
        return;
      }
      IWorkbenchPage page = part.getSite().getPage();
      ISelection selection = event.getContext();
      if (selection instanceof IStructuredSelection) {
        Object firstElement = ((IStructuredSelection) selection).getFirstElement();
        if (firstElement instanceof IAdaptable) {
          IAdaptable adaptable = (IAdaptable) firstElement;

          EvaluateContext frame = (EvaluateContext) adaptable.getAdapter(EvaluateContext.class);
          if (frame != null) {
            putStackFrame(page, frame);
            return;
          }
        }
      }
      // debug context for the |page| has been lost
      removeStackFrame(page);
    }
  }

  /**
   * Returns the stackframe corresponding to the given {@code part}, or {@code
   * null} if none.
   *
   * @param part the active part
   * @return the stack frame in whose context the evaluation is performed, or
   *         {@code null} if none
   */
  public static EvaluateContext getStackFrameFor(IWorkbenchPart part) {
    IWorkbenchPage page = part.getSite().getPage();
    EvaluateContext frame = getStackFrameFor(page);
    if (frame == null) {
      return getStackFrameFor(page.getWorkbenchWindow());
    }
    return frame;
  }

  /**
   * Returns the stackframe corresponding to the given {@code window}, or
   * {@code null} if none.
   *
   * @param window to find the StackFrame for. If {@code null}, the {@code
   *        activeWindow} will be used instead
   * @return the stack frame in whose the evaluation is performed, or {@code
   *         null} if none
   */
  public static EvaluateContext getStackFrameFor(IWorkbenchWindow window) {
    Set<IWorkbenchWindow> visitedWindows = new HashSet<IWorkbenchWindow>();
    if (window == null) {
      window = instance.activeWindow;
    }
    return getStackFrameFor(window, visitedWindows);
  }

  private static EvaluateContext getStackFrameFor(
      IWorkbenchWindow window, Set<IWorkbenchWindow> visitedWindows) {
    IWorkbenchPage activePage = window.getActivePage();
    EvaluateContext frame = null;
    // Check the active page in the window
    if (activePage != null) {
      frame = getStackFrameFor(activePage);
      if (frame != null) {
        return frame;
      }
    }
    // Check all the current Eclipse window pages
    for (IWorkbenchPage windowPage : window.getPages()) {
      if (activePage != windowPage) {
        frame = getStackFrameFor(windowPage);
        if (frame != null) {
          return frame;
        }
      }
    }

    // Last resort - check all other Eclipse windows
    visitedWindows.add(window);

    for (IWorkbenchWindow workbenchWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
      if (!visitedWindows.contains(workbenchWindow)) {
        frame = getStackFrameFor(workbenchWindow, visitedWindows);
        if (frame != null) {
          return frame;
        }
      }
    }

    // Nothing found
    return null;
  }

  private static EvaluateContext getStackFrameFor(IWorkbenchPage page) {
    if (instance != null) {
      return instance.pageToFrame.get(page);
    }
    return null;
  }

  private void removeStackFrame(IWorkbenchPage page) {
    pageToFrame.remove(page);
    if (pageToFrame.isEmpty()) {
      // No more available frames
      System.setProperty(DEBUGGER_ACTIVE, Boolean.FALSE.toString());
    }
  }

  private void putStackFrame(IWorkbenchPage page, EvaluateContext frame) {
    pageToFrame.put(page, frame);
    System.setProperty(DEBUGGER_ACTIVE, Boolean.TRUE.toString());
  }

}
