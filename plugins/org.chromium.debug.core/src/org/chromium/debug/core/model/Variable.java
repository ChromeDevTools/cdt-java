// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.AbstractList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.FunctionScopeExtension;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsEvaluateContext.ResultOrException;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObjectProperty;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsScope.WithScope;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter;

/**
 * An IVariable implementation over a JsVariable instance. This is class is a base implementation,
 * and it contains several concrete implementations as nested classes.
 */
public abstract class Variable extends DebugElementImpl.WithEvaluate implements IVariable {

  /**
   * Wraps {@link JsVariable}. It extracts its {@link JsValue} if possible or provides error
   * message as a {@link Value}.
   */
  public static Variable forRealValue(EvaluateContext evaluateContext, JsVariable jsVariable,
      boolean isInternalProperty, ExpressionTracker.Node trackerNode) {
    ValueBase value;
    if (jsVariable.isReadable()) {
      JsValue jsValue = jsVariable.getValue();
      if (jsValue == null) {
        JsObjectProperty objectProperty = jsVariable.asObjectProperty();
        if (objectProperty == null) {
          value = new ValueBase.ErrorMessageValue(evaluateContext,
              "Variable value is unavailable");
        } else {
          // This is blocking. Consider making this call async and the entire method async
          // to parallel if for several properties.
          value = calculateAccessorPropertyBlocking(objectProperty, evaluateContext, trackerNode);
          if (value == null) {
            value = new ValueBase.ErrorMessageValue(evaluateContext, "Unreadable object property");
          }
        }
      } else {
        value = Value.create(evaluateContext, jsValue, trackerNode);
      }
    } else {
      value = new ValueBase.ErrorMessageValue(evaluateContext, "Unreadable variable");
    }

    return new Real(evaluateContext, jsVariable, value, isInternalProperty, trackerNode);
  }

  private static ValueBase calculateAccessorPropertyBlocking(final JsObjectProperty property,
      final EvaluateContext evaluateContext, final ExpressionTracker.Node expressionTrackerNode) {
    if (property.getGetterAsFunction() == null) {
      return new ValueBase.ErrorMessageValue(evaluateContext, "Property has undefined getter");
    }
    class Callback implements JsEvaluateContext.EvaluateCallback {
      ValueBase valueBase = null;

      @Override
      public void success(ResultOrException result) {
        valueBase = result.accept(new ResultOrException.Visitor<ValueBase>() {
          @Override public ValueBase visitResult(JsValue value) {
            return Value.create(evaluateContext, value, expressionTrackerNode);
          }
          @Override public ValueBase visitException(JsValue exception) {
            return new ValueBase.ErrorMessageValue(evaluateContext, "Evaluate failure", exception);
          }
        });
      }

      @Override public void failure(String errorMessage) {
        valueBase = new ValueBase.ErrorMessageValue(evaluateContext,
            "Failed to evaluate property value: " + errorMessage);
      }
    }
    Callback callback = new Callback();
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    RelayOk relayOk = property.evaluateGet(callback, callbackSemaphore);
    callbackSemaphore.acquireDefault(relayOk);
    return callback.valueBase;
  }

  public static Variable forException(EvaluateContext evaluateContext,
      JsValue exceptionValue) {
    Value value = Value.create(evaluateContext, exceptionValue,
        ExpressionTracker.NO_EXPRESSION_NODE);
    return new Variable.Virtual(evaluateContext, "<exception>", JAVASCRIPT_REFERENCE_TYPE_NAME,
        value, null);
  }

  public static Variable forScope(EvaluateContext evaluateContext, JsScope scope,
      ExpressionTracker.Node expressionNode) {
    String scopeVariableName = "<" + scope.getType() + ">";
    ValueBase scopeValue = new ValueBase.ScopeValue(evaluateContext, scope, expressionNode);
    return forScopeImpl(evaluateContext, scopeVariableName, scopeValue);
  }

  public static Variable forWithScope(EvaluateContext evaluateContext,
      WithScope withScope, ExpressionTracker.Node expressionNode) {
    Value value = Value.create(evaluateContext, withScope.getWithArgument(), expressionNode);
    return forScopeImpl(evaluateContext, "<with>", value);
  }

  private static Variable forScopeImpl(EvaluateContext evaluateContext, String scopeName,
      ValueBase scopeValue) {
    return new Variable.Virtual(evaluateContext, scopeName, "<scope>", scopeValue, null);
  }

  public static Variable forFunctionScopes(EvaluateContext evaluateContext,
      final JsFunction jsFunction, final FunctionScopeExtension functionScopeExtension) {
    ValueBase value = new ValueBase.ValueWithLazyVariables(evaluateContext) {
      @Override public String getReferenceTypeName() throws DebugException {
        return "<function scope>";
      }

      @Override public boolean isAllocated() throws DebugException {
        return true;
      }

      @Override public boolean hasVariables() throws DebugException {
        return !functionScopeExtension.getScopes(jsFunction).isEmpty();
      }

      @Override protected IVariable[] calculateVariables() {
        List<? extends JsScope> list = functionScopeExtension.getScopes(jsFunction);
        // Put scopes in the opposite order: innermost first.
        // Closure tends to be parameterized by the innermost variable at most.
        List<? extends JsScope> reverseList = reverseList(list);
        return StackFrame.wrapScopes(getEvaluateContext(), reverseList, null,
            ExpressionTracker.FUNCTION_SCOPE_FACTORY);
      }

      @Override public Value asRealValue() {
        return null;
      }

      @Override public String getValueString() {
        return "";
      }

      private <T> List<T> reverseList(final List<T> input) {
        return new AbstractList<T>() {
          @Override
          public T get(int index) {
            return input.get(input.size() - index - 1);
          }
          @Override
          public int size() {
            return input.size();
          }
        };
      }
    };

    return forScopeImpl(evaluateContext, "<function scope>", value);
  }

  public static Variable forEvaluateExpression(EvaluateContext evaluateContext, JsValue jsValue,
      String expression) {
    ExpressionTracker.Node expressionTrackerNode =
        ExpressionTracker.createExpressionNode(expression);
    ValueBase value = Value.create(evaluateContext, jsValue, expressionTrackerNode);
    return new Variable.Virtual(evaluateContext, expression, JAVASCRIPT_REFERENCE_TYPE_NAME, value,
        expressionTrackerNode.calculateQualifiedName());
  }

  /**
   * Represents a real variable -- wraps {@link JsVariable}.
   */
  public static class Real extends Variable {
    private final JsVariable jsVariable;
    private final ExpressionTracker.Node expressionTrackerNode;

    /**
     * Specifies whether this variable is internal property (__proto__ etc).
     * TODO(peter.rybin): use it in UI.
     */
    private final boolean isInternalProperty;

    Real(EvaluateContext evaluateContext, JsVariable jsVariable, ValueBase value,
        boolean isInternalProperty, ExpressionTracker.Node expressionTrackerNode) {
      super(evaluateContext, value);
      this.jsVariable = jsVariable;
      this.isInternalProperty = isInternalProperty;
      this.expressionTrackerNode = expressionTrackerNode;
    }

    @Override public String getName() {
      return jsVariable.getName();
    }
    @Override public String getReferenceTypeName() {
      return JAVASCRIPT_REFERENCE_TYPE_NAME;
    }
    @Override public Real asRealVariable() {
      return this;
    }
    public JsVariable getJsVariable() {
      return jsVariable;
    }
    @Override public String createWatchExpression() {
      return expressionTrackerNode.calculateQualifiedName();
    }
    public String createHolderWatchExpression() {
      return expressionTrackerNode.calculateParentQualifiedName();
    }
  }

  /**
   * Represents some auxiliary variable. Its name and reference type are provided by a caller.
   */
  private static class Virtual extends Variable {
    private final String name;
    private final String referenceTypeName;
    private final String watchExpression;

    Virtual(EvaluateContext evaluateContext, String name, String referenceTypeName,
        ValueBase value, String watchExpression) {
      super(evaluateContext, value);
      this.name = name;
      this.referenceTypeName = referenceTypeName;
      this.watchExpression = watchExpression;
    }

    @Override public String getName() {
      return name;
    }
    @Override public String getReferenceTypeName() {
      return referenceTypeName;
    }
    @Override public Real asRealVariable() {
      return null;
    }
    @Override protected String createWatchExpression() {
      return watchExpression;
    }
  }

  private final ValueBase value;

  protected Variable(EvaluateContext evaluateContext, ValueBase value) {
    super(evaluateContext);
    this.value = value;
  }

  @Override public abstract String getName();

  @Override public abstract String getReferenceTypeName();

  @Override public ValueBase getValue() {
    return value;
  }

  @Override public boolean hasValueChanged() throws DebugException {
    return false;
  }

  public void setValue(String expression) throws DebugException {
  }

  public void setValue(IValue value) throws DebugException {
  }

  public boolean supportsValueModification() {
    return false; // TODO(apavlov): fix once V8 supports it
  }

  public boolean verifyValue(IValue value) throws DebugException {
    return verifyValue(value.getValueString());
  }

  public boolean verifyValue(String expression) {
    return true;
  }

  public boolean verifyValue(JsValue value) {
    return verifyValue(value.getValueString());
  }

  /**
   * @return expression or null
   */
  protected abstract String createWatchExpression();

  public abstract Real asRealVariable();

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter) {
    if (IWatchExpressionFactoryAdapter.class == adapter) {
      return EXPRESSION_FACTORY_ADAPTER;
    }
    return super.getAdapter(adapter);
  }

  private final static IWatchExpressionFactoryAdapter EXPRESSION_FACTORY_ADAPTER =
      new IWatchExpressionFactoryAdapter() {
    public String createWatchExpression(IVariable variable) throws CoreException {
      Variable castVariable = (Variable) variable;
      String expressionText = castVariable.createWatchExpression();
      if (expressionText == null) {
        throw new CoreException(new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
            Messages.Variable_CANNOT_BUILD_EXPRESSION));
      }
      return expressionText;
    }
  };

  /**
   * A type of JavaScript reference. All JavaScript references have no type.
   */
  private static final String JAVASCRIPT_REFERENCE_TYPE_NAME = "";
}
