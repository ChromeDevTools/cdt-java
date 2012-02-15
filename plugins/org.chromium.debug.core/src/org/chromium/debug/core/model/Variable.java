// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsScope.WithScope;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
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
      boolean isInternalProperty, Real.HostObject hostObject) {
    ValueBase value;
    if (jsVariable.isReadable()) {
      JsValue jsValue = jsVariable.getValue();
      if (jsValue == null) {
        value = new ValueBase.ErrorMessageValue(evaluateContext, "Variable value is unavailable");
      } else {
        SelfAsHostObject selfAsHostObject = new SelfAsHostObject(jsVariable);
        value = Value.create(evaluateContext, jsValue, selfAsHostObject);
      }
    } else {
      value = new ValueBase.ErrorMessageValue(evaluateContext, "Unreadable variable");
    }

    return new Real(evaluateContext, jsVariable, value, isInternalProperty, hostObject);
  }

  public static Variable forException(EvaluateContext evaluateContext,
      ExceptionData exceptionData) {
    Value value = Value.create(evaluateContext, exceptionData.getExceptionValue(), null);
    return new Variable.Virtual(evaluateContext, "<exception>", JAVASCRIPT_REFERENCE_TYPE_NAME,
        value);
  }

  public static Variable forScope(EvaluateContext evaluateContext, JsScope scope,
      ValueBase.ValueAsHostObject selfAsHostObject) {
    ValueBase scopeValue = new ValueBase.ScopeValue(evaluateContext, scope, selfAsHostObject);
    String scopeVariableName = "<" + scope.getType() + ">";
    return forScope(evaluateContext, scopeVariableName, scopeValue);
  }

  public static Variable forWithScope(EvaluateContext evaluateContext,
      WithScope withScope) {
    Value value = Value.create(evaluateContext, withScope.getWithArgument(), null);
    return forScope(evaluateContext, "<with>", value);
  }

  private static Variable forScope(EvaluateContext evaluateContext, String scopeName,
      ValueBase scopeValue) {
    return new Variable.Virtual(evaluateContext, scopeName, "<scope>", scopeValue);
  }

  /**
   * Represents a real variable -- wraps {@link JsVariable}.
   */
  public static class Real extends Variable {
    private final JsVariable jsVariable;
    private final HostObject hostObject;

    /**
     * Specifies whether this variable is internal property (__proto__ etc).
     * TODO(peter.rybin): use it in UI.
     */
    private final boolean isInternalProperty;

    Real(EvaluateContext evaluateContext, JsVariable jsVariable,
        ValueBase value, boolean isInternalProperty, HostObject hostObject) {
      super(evaluateContext, value);
      this.jsVariable = jsVariable;
      this.isInternalProperty = isInternalProperty;
      this.hostObject = hostObject;
    }

    @Override public String getName() {
      return jsVariable.getName();
    }
    @Override public String getReferenceTypeName() {
      return JAVASCRIPT_REFERENCE_TYPE_NAME;
    }
    @Override protected String createWatchExpression() {
      return jsVariable.getFullyQualifiedName();
    }
    @Override public Real asRealVariable() {
      return this;
    }
    public JsVariable getJsVariable() {
      return jsVariable;
    }
    public HostObject getHostObject() {
      return hostObject;
    }

    /**
     * If variable is a property of some object, it need an access to this object. This is used
     * to build an expression for getting property descriptor.
     */
    public interface HostObject {
      /**
       * @return a JavaScript descriptor that return a value of that object -- the same that
       *     {@link JsVariable#getFullyQualifiedName()} returns
       */
      String getExpression();
    }
  }

  /**
   * Represents some auxiliary variable. Its name and reference type are provided by a caller.
   */
  private static class Virtual extends Variable {
    private final String name;
    private final String referenceTypeName;

    Virtual(EvaluateContext evaluateContext, String name, String referenceTypeName,
        ValueBase value) {
      super(evaluateContext, value);
      this.name = name;
      this.referenceTypeName = referenceTypeName;
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
      return null;
    }
  }

  /**
   * Implements ValueAsHostObject based on JsVariable. This goes to the
   * corresponding Value instance.
   */
  private static class SelfAsHostObject implements ValueBase.ValueAsHostObject {
    private final JsVariable jsVariable;

    SelfAsHostObject(JsVariable jsVariable) {
      this.jsVariable = jsVariable;
    }

    @Override
    public String getExpression() {
      return jsVariable.getFullyQualifiedName();
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
