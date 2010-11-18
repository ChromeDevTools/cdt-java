// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.Value;
import org.chromium.debug.core.model.Variable;
import org.chromium.debug.core.model.VmResourceId;
import org.chromium.debug.core.sourcemap.SourcePosition;
import org.chromium.debug.core.sourcemap.SourcePositionMap;
import org.chromium.debug.core.sourcemap.SourcePositionMap.TranslateDirection;
import org.chromium.debug.ui.JsDebugModelPresentation;
import org.chromium.debug.ui.editors.JsEditor;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.TextStreamPosition;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The action for context menu in Variable/Expression views that opens selected
 * function source text in editor.
 */
public abstract class OpenFunctionAction implements IObjectActionDelegate,
    IActionDelegate2 {
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

  interface VariableWrapper {
    JsValue getJsValue();
    IDebugElement getDebugElement();
    DebugTargetImpl getDebugTarget();
  }

  private final ElementHandler elementHandler;
  private Runnable currentRunnable = null;

  protected OpenFunctionAction(ElementHandler elementHandler) {
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

  public void selectionChanged(IAction action, ISelection selection) {
    VariableWrapper wrapper = getElementFromSelection(selection, elementHandler);
    currentRunnable = createRunnable(wrapper);
    action.setEnabled(currentRunnable != null);
  }

  private Runnable createRunnable(VariableWrapper wrapper) {
    if (wrapper == null) {
      return null;
    }
    final DebugTargetImpl debugTarget = wrapper.getDebugTarget();
    if (debugTarget == null) {
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

        Script script = jsFunction.getScript();
        if (script == null) {
          return;
        }
        ISourceLocator sourceLocator = debugTarget.getLaunch().getSourceLocator();
        if (sourceLocator instanceof ISourceLookupDirector == false) {
          return;
        }
        ISourceLookupDirector director = (ISourceLookupDirector) sourceLocator;
        Object sourceObject = director.getSourceElement(script);
        if (sourceObject instanceof IFile == false) {
          return;
        }
        IFile resource = (IFile) sourceObject;
        IEditorInput input = JsDebugModelPresentation.toEditorInput(resource);
        IEditorPart editor;
        try {
          editor = activeWorkbenchWindow.getActivePage().openEditor(input, JsEditor.EDITOR_ID);
        } catch (PartInitException e) {
          throw new RuntimeException(e);
        }
        if (editor instanceof ITextEditor == false) {
          return;
        }
        ITextEditor textEditor = (ITextEditor) editor;
        TextStreamPosition openParenPosition = jsFunction.getOpenParenPosition();
        if (openParenPosition == null) {
          return;
        }

        SourcePositionMap positionMap = debugTarget.getSourcePositionMap();
        SourcePosition originalPosition = positionMap.translatePosition(
            VmResourceId.forScript(script), openParenPosition.getLine(),
            openParenPosition.getColumn(), TranslateDirection.VM_TO_USER);
        int offset = calculateOffset(textEditor, originalPosition);

        textEditor.selectAndReveal(offset, 0);
      }

      private int calculateOffset(ITextEditor editor, SourcePosition originalPosition) {
        IDocumentProvider provider = editor.getDocumentProvider();
        IDocument document = provider.getDocument(editor.getEditorInput());
        int lineStartOffset;
        try {
          lineStartOffset = document.getLineOffset(originalPosition.getLine());
        } catch (BadLocationException e) {
          throw new RuntimeException(e);
        }
        return lineStartOffset + originalPosition.getColumn();
      }
    };
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

  private JsFunction getJsFunctionFromElement(VariableWrapper wrapper) {
    if (wrapper == null) {
      return null;
    }
    JsValue jsValue = wrapper.getJsValue();
    if (jsValue == null) {
      return null;
    }
    JsObject jsObject = jsValue.asObject();
    if (jsObject == null) {
      return null;
    }
    return jsObject.asFunction();
  }

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

  static abstract class ElementHandler {
    protected abstract VariableWrapper castElement(Object element);
  }

  static final ElementHandler VARIABLE_VIEW_ELEMENT_HANDLER = new ElementHandler() {
    @Override protected VariableWrapper castElement(Object element) {
      if (element instanceof Variable == false) {
        return null;
      }
      final Variable variable = (Variable) element;
      return new VariableWrapper() {
        public JsValue getJsValue() {
          JsVariable jsVariable = variable.getJsVariable();
          return jsVariable.getValue();
        }
        public IDebugElement getDebugElement() {
          return variable;
        }
        public DebugTargetImpl getDebugTarget() {
          return variable.getDebugTarget();
        }
      };
    }
  };
  static final ElementHandler EXPRESSION_VIEW_ELEMENT_HANDLER = new ElementHandler() {
    @Override protected VariableWrapper castElement(Object element) {
      if (element instanceof IWatchExpression == false) {
        return null;
      }
      final IWatchExpression watchExpression = (IWatchExpression) element;
      return new VariableWrapper() {
        public JsValue getJsValue() {
          IValue value = watchExpression.getValue();
          if (value instanceof Value == false) {
            return null;
          }
          Value chromiumValue = (Value) value;
          return chromiumValue.getJsValue();
        }
        public IDebugElement getDebugElement() {
          return watchExpression;
        }
        public DebugTargetImpl getDebugTarget() {
          IDebugTarget debugTarget = watchExpression.getDebugTarget();
          if (debugTarget instanceof DebugTargetImpl == false) {
            return null;
          }
          return (DebugTargetImpl) debugTarget;
        }
      };
    }
  };
}
