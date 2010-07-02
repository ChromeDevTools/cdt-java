package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.Value;
import org.chromium.debug.core.model.Variable;
import org.chromium.debug.ui.JsDebugModelPresentation;
import org.chromium.debug.ui.editors.JsEditor;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
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
public abstract class OpenFunctionAction<ELEMENT> implements IObjectActionDelegate,
    IActionDelegate2 {
  public static class ForVariable extends OpenFunctionAction<Variable> {
    @Override
    protected Variable castElement(Object element) {
      if (element instanceof Variable == false) {
        return null;
      }
      return (Variable) element;
    }
    @Override
    protected JsValue getJsValue(Variable variable) {
      JsVariable jsVariable = variable.getJsVariable();
      return jsVariable.getValue();
    }
    @Override
    protected DebugTargetImpl getDebugTarget(Variable variable) {
      return variable.getDebugTarget();
    }
  }
  public static class ForExpression extends OpenFunctionAction<IWatchExpression> {
    @Override
    protected IWatchExpression castElement(Object element) {
      if (element instanceof IWatchExpression == false) {
        return null;
      }
      return (IWatchExpression) element;
    }
    @Override
    protected DebugTargetImpl getDebugTarget(IWatchExpression expression) {
      IDebugTarget debugTarget = expression.getDebugTarget();
      if (debugTarget instanceof DebugTargetImpl == false) {
        return null;
      }
      return (DebugTargetImpl) debugTarget;
    }
    @Override
    protected JsValue getJsValue(IWatchExpression expression) {
      IValue value = expression.getValue();
      if (value instanceof Value == false) {
        return null;
      }
      Value chromiumValue = (Value) value;
      return chromiumValue.getJsValue();
    }
  }

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
    final ELEMENT variable = getElementFromSelection(selection);
    final JsFunction jsFunction = getJsFunctionFromElement(variable);

    currentRunnable = createRunnable(variable, jsFunction);
    action.setEnabled(currentRunnable != null);
  }

  private Runnable createRunnable(final ELEMENT element, final JsFunction jsFunction) {
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
        DebugTargetImpl debugTarget = getDebugTarget(element);
        if (debugTarget == null) {
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

  private JsFunction getJsFunctionFromElement(ELEMENT element) {
    if (element == null) {
      return null;
    }
    JsValue jsValue = getJsValue(element);
    if (jsValue == null) {
      return null;
    }
    JsObject jsObject = jsValue.asObject();
    if (jsObject == null) {
      return null;
    }
    return jsObject.asFunction();
  }

  private ELEMENT getElementFromSelection(ISelection selection) {
    if (selection instanceof IStructuredSelection == false) {
      return null;
    }
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    if (structuredSelection.size() != 1) {
      // We do not support multiple selection.
      return null;
    }
    Object element = structuredSelection.getFirstElement();
    ELEMENT typedElement = castElement(element);
    return typedElement;
  }

  protected abstract ELEMENT castElement(Object element);

  protected abstract JsValue getJsValue(ELEMENT element);

  protected abstract DebugTargetImpl getDebugTarget(ELEMENT element);
}
