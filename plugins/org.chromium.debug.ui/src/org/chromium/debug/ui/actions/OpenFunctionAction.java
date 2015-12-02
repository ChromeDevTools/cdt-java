// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.Value;
import org.chromium.debug.core.model.VmResourceId;
import org.chromium.debug.core.sourcemap.SourcePosition;
import org.chromium.debug.core.sourcemap.SourcePositionMap;
import org.chromium.debug.core.sourcemap.SourcePositionMap.TranslateDirection;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.Script;
import org.chromium.sdk.TextStreamPosition;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The action for context menu in Variable/Expression views that opens selected
 * function source text in editor.
 */
public abstract class OpenFunctionAction extends VariableBasedAction {
  public static class ForVariable extends OpenFunctionAction {
    public ForVariable() {
      super(VARIABLE_VIEW_ELEMENT_HANDLER);
    }
  }
  public static class ForExpression extends OpenFunctionAction {
    public ForExpression() {
      super(EXPRESSION_VIEW_ELEMENT_HANDLER);
    }
  }

  protected OpenFunctionAction(ElementHandler elementHandler) {
    super(elementHandler);
  }

  protected Runnable createRunnable(VariableWrapper wrapper) {
    if (wrapper == null) {
      return null;
    }
    final ConnectedTargetData connectedTargetData = wrapper.getConnectedTargetData();
    if (connectedTargetData == null) {
      return null;
    }
    final JsFunction jsFunction = getJsFunctionFromElement(wrapper);
    if (jsFunction == null) {
      return null;
    }
    return new Runnable() {

      public void run() {
        // This works in UI thread.
        IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();

        ISourceLocator sourceLocator =
            connectedTargetData.getDebugTarget().getLaunch().getSourceLocator();
        if (sourceLocator instanceof ISourceLookupDirector == false) {
          return;
        }
        ISourceLookupDirector director = (ISourceLookupDirector) sourceLocator;


        SourcePositionMap positionMap = connectedTargetData.getSourcePositionMap();
        SourcePosition userPosition;
        {
          // First get VM positions.
          Script script = jsFunction.getScript();
          if (script == null) {
            return;
          }
          TextStreamPosition functionOpenParenPosition = jsFunction.getOpenParenPosition();
          if (functionOpenParenPosition == null) {
            return;
          }

          // Convert them to user positions.
          userPosition = positionMap.translatePosition(
              VmResourceId.forScript(script), functionOpenParenPosition.getLine(),
              functionOpenParenPosition.getColumn(), TranslateDirection.VM_TO_USER);
        }

        Object sourceObject = director.getSourceElement(userPosition.getId());
        if (sourceObject instanceof IFile == false) {
          return;
        }
        IFile resource = (IFile) sourceObject;
        IEditorPart editor;
        try {
          editor= IDE.openEditor(activeWorkbenchWindow.getActivePage(), resource, true);
        } catch (PartInitException e) {
          throw new RuntimeException(e);
        }
        if (editor instanceof ITextEditor == false) {
          return;
        }
        ITextEditor textEditor = (ITextEditor) editor;

        int offset = calculateOffset(textEditor, userPosition);

        textEditor.selectAndReveal(offset, 0);
      }

      private int calculateOffset(ITextEditor editor, SourcePosition userPosition) {
        IDocumentProvider provider = editor.getDocumentProvider();
        IDocument document = provider.getDocument(editor.getEditorInput());
        int lineStartOffset;
        try {
          lineStartOffset = document.getLineOffset(userPosition.getLine());
        } catch (BadLocationException e) {
          throw new RuntimeException(e);
        }
        return lineStartOffset + userPosition.getColumn();
      }
    };
  }

  private JsFunction getJsFunctionFromElement(VariableWrapper wrapper) {
    if (wrapper == null) {
      return null;
    }
    final Value uiValue = wrapper.getValue();
    if (uiValue == null) {
      // Probably hasn't got result yet.
      return null;
    }
    JsValue jsValue = uiValue.getJsValue();
    if (jsValue == null) {
      return null;
    }
    JsObject jsObject = jsValue.asObject();
    if (jsObject == null) {
      return null;
    }
    return jsObject.asFunction();
  }
}
