package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.Variable;
import org.chromium.debug.ui.JsDebugModelPresentation;
import org.chromium.debug.ui.editors.JsEditor;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The action for context view in Variable view that opens selected function source text in editor.
 */
public class OpenFunctionAction implements IObjectActionDelegate, IActionDelegate2 {
  private Runnable currentRunnable = null;

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void run(IAction action) {
    if (currentRunnable == null) {
      return;
    }
    currentRunnable.run();
  }

  public void selectionChanged(IAction action, ISelection selection) {
    final Variable variable = getVariableFromSelection(selection);
    final JsFunction jsFunction = getJsFunctionFromVariable(variable);

    currentRunnable = createRunnable(variable, jsFunction);
    action.setEnabled(currentRunnable != null);
  }

  private Runnable createRunnable(final Variable variable, final JsFunction jsFunction) {
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
        IFile resource = variable.getDebugTarget().getScriptResource(script);
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
        textEditor.selectAndReveal(jsFunction.getSourcePosition(), 0);
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

  private JsFunction getJsFunctionFromVariable(Variable variable) {
    if (variable == null) {
      return null;
    }
    JsVariable jsVariable = variable.getJsVariable();
    JsValue jsValue = jsVariable.getValue();
    JsObject jsObject = jsValue.asObject();
    if (jsObject == null) {
      return null;
    }
    return jsObject.asFunction();
  }

  private Variable getVariableFromSelection(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    if (structuredSelection.size() != 1) {
      // We do not support multiple selection.
      return null;
    }
    Object element = structuredSelection.getFirstElement();
    if (element instanceof Variable == false) {
      return null;
    }
    return (Variable) element;
  }
}
