// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.EvaluateContext;
import org.chromium.debug.ui.ChromiumDebugUIPlugin;
import org.chromium.debug.ui.JsEvalContextManager;
import org.chromium.debug.ui.editors.JavascriptUtil;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsVariable;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.ui.DebugPopup;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.InspectPopupDialog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Action for inspecting a JavaScript snippet.
 */
public class JsInspectSnippetAction implements IEditorActionDelegate,
    IWorkbenchWindowActionDelegate, IPartListener, IViewActionDelegate,
    JsEvaluateContext.EvaluateCallback {

  private static final String ACTION_DEFINITION_ID = "org.chromium.debug.ui.commands.Inspect"; //$NON-NLS-1$

  private IWorkbenchWindow window;

  private IWorkbenchPart targetPart;

  private IAction action;

  private String selectedText;

  private IExpression expression;

  private ITextEditor textEditor;

  private ISelection originalSelection;

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    this.action = action;
    setTargetPart(targetEditor);
  }

  public void run(IAction action) {
    updateAction();
    run();
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.action = action;
  }

  public void dispose() {
    IWorkbenchWindow win = getWindow();
    if (win != null) {
      win.getPartService().removePartListener(this);
    }
  }

  private IWorkbenchWindow getWindow() {
    return window;
  }

  public void init(IWorkbenchWindow window) {
    this.window = window;
    IWorkbenchPage page = window.getActivePage();
    if (page != null) {
      setTargetPart(page.getActivePart());
    }
    window.getPartService().addPartListener(this);
  }

  public void partActivated(IWorkbenchPart part) {
    setTargetPart(part);
  }

  public void partBroughtToTop(IWorkbenchPart part) {

  }

  public void partClosed(IWorkbenchPart part) {
    if (part == getTargetPart()) {
      setTargetPart(null);
    }
  }

  private IWorkbenchPart getTargetPart() {
    return targetPart;
  }

  public void partDeactivated(IWorkbenchPart part) {
  }

  public void partOpened(IWorkbenchPart part) {
  }

  private void setTargetPart(IWorkbenchPart part) {
    this.targetPart = part;
  }

  public void init(IViewPart view) {
    setTargetPart(view);
  }

  private void updateAction() {
    if (action != null) {
      retrieveSelection();
    }
  }

  protected ISelection getTargetSelection() {
    IWorkbenchPart part = getTargetPart();
    if (part != null) {
      ISelectionProvider provider = part.getSite().getSelectionProvider();
      if (provider != null) {
        return provider.getSelection();
      }
    }
    return null;
  }

  private EvaluateContext getStackFrameContext() {
    IAdaptable testContext = DebugUITools.getDebugContext();

    IWorkbenchPart part = getTargetPart();
    return getStackFrameForPart(part);
  }

  private EvaluateContext getStackFrameForPart(IWorkbenchPart part) {
    EvaluateContext frame = part == null
        ? JsEvalContextManager.getStackFrameFor(getWindow())
        : JsEvalContextManager.getStackFrameFor(part);
    return frame;
  }

  private void run() {
    getStackFrameContext().getJsEvaluateContext().evaluateAsync(getSelectedText(), this, null);
  }

  protected String getSelectedText() {
    return selectedText;
  }

  protected Shell getShell() {
    if (getTargetPart() != null) {
      return getTargetPart().getSite().getShell();
    }
    return ChromiumDebugUIPlugin.getActiveWorkbenchShell();
  }

  private void retrieveSelection() {
    ISelection targetSelection = getTargetSelection();
    if (targetSelection instanceof ITextSelection) {
      ITextSelection ts = (ITextSelection) targetSelection;
      String text = ts.getText();
      if (textHasContent(text)) {
        selectedText = text;
      } else if (getTargetPart() instanceof IEditorPart) {
        IEditorPart editor = (IEditorPart) getTargetPart();
        if (editor instanceof ITextEditor) {
          selectedText = extractSurroundingWord(ts, (ITextEditor) editor);
        }
      }
    }
  }

  private String extractSurroundingWord(ITextSelection targetSelection, ITextEditor editor) {
    return JavascriptUtil.extractSurroundingJsIdentifier(
        editor.getDocumentProvider().getDocument(editor.getEditorInput()),
        targetSelection.getOffset());
  }

  private boolean textHasContent(String text) {
    return text != null && JavascriptUtil.ID_PATTERN.matcher(text).find();
  }

  public void success(JsVariable var) {
    if (ChromiumDebugUIPlugin.getDefault() == null) {
      return;
    }
    if (var != null) {
      if (ChromiumDebugUIPlugin.getDisplay().isDisposed()) {
        return;
      }
      displayResult(var, null);
    }
  }

  public void failure(String errorMessage) {
    displayResult(null, errorMessage);
  }

  protected void displayResult(final JsVariable var, String errorMessage) {
    IWorkbenchPart part = getTargetPart();
    final StyledText styledText = getStyledText(part);
    if (styledText == null) {
      return; // TODO(apavlov): fix this when adding inspected variables
    } else {
      expression = new JsInspectExpression(getStackFrameContext(), selectedText, var, errorMessage);
      ChromiumDebugUIPlugin.getDisplay().asyncExec(new Runnable() {
        public void run() {
          showPopup(styledText);
        }
      });
    }
  }

  protected void showPopup(StyledText textWidget) {
    IWorkbenchPart part = getTargetPart();
    if (part instanceof ITextEditor) {
      textEditor = (ITextEditor) part;
      originalSelection = getTargetSelection();
    }
    DebugPopup displayPopup =
        new InspectPopupDialog(getShell(), getPopupAnchor(textWidget), ACTION_DEFINITION_ID,
            expression) {
          @Override
          public boolean close() {
            boolean returnValue = super.close();
            if (textEditor != null && originalSelection != null) {
              textEditor.getSelectionProvider().setSelection(originalSelection);
              textEditor = null;
              originalSelection = null;
            }
            return returnValue;
          }
        };
    displayPopup.open();
  }

  private StyledText getStyledText(IWorkbenchPart part) {
    ITextViewer viewer = (ITextViewer) part.getAdapter(ITextViewer.class);
    StyledText textWidget = null;
    if (viewer == null) {
      Control control = (Control) part.getAdapter(Control.class);
      if (control instanceof StyledText) {
        textWidget = (StyledText) control;
      }
    } else {
      textWidget = viewer.getTextWidget();
    }
    return textWidget;
  }

  private static Point getPopupAnchor(StyledText textWidget) {
    if (textWidget != null) {
      Point docRange = textWidget.getSelectionRange();
      int midOffset = docRange.x + (docRange.y / 2);
      Point point = textWidget.getLocationAtOffset(midOffset);
      point = textWidget.toDisplay(point);
      point.y += getFontHeight(textWidget);
      return point;
    }
    return null;
  }

  private static int getFontHeight(StyledText textWidget) {
    GC gc = new GC(textWidget);
    gc.setFont(textWidget.getFont());
    int height = gc.getFontMetrics().getHeight();
    gc.dispose();
    return height;
  }

}
