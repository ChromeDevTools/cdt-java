// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.RelaySyncCallback;
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
   * Represents a real variable -- wraps {@link JsVariable}.
   * TODO: consider hiding this public class behind a static factory method.
   */
  public static class Real extends Variable {
    private final JsVariable variable;

    /**
     * Specifies whether this variable is internal property (__proto__ etc).
     * TODO(peter.rybin): use it in UI.
     */
    private final boolean isInternalProperty;

    public Real(EvaluateContext evaluateContext, JsVariable variable,
        boolean isInternalProperty) {
      super(evaluateContext);
      this.variable = variable;
      this.isInternalProperty = isInternalProperty;
    }

    public String getName() {
      return variable.getName();
    }

    public String getReferenceTypeName() throws DebugException {
      return variable.getValue().getType().toString();
    }

    protected Value createValue() {
      JsValue value = variable.isReadable()
          ? variable.getValue()
          : null;
      if (value == null) {
        return null;
      }
      return Value.create(getEvaluateContext(), value);
    }

    @Override
    protected String createWatchExpression() {
      return variable.getFullyQualifiedName();
    }
  }

  /**
   * Represents a scope as a variable, serves for grouping real variables in UI view.
   * TODO: consider hiding this public class behind a static factory method.
   */
  public static class ScopeWrapper extends Variable {
    private final JsScope jsScope;

    public ScopeWrapper(EvaluateContext evaluateContext, JsScope scope) {
      super(evaluateContext);
      this.jsScope = scope;
    }

    @Override
    public String getName() {
      return "<" + jsScope.getType() + ">";
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
      return "<scope>";
    }

    @Override
    protected Value createValue() {
      JsValue scopeValue = new ScopeObjectVariable();
      return Value.create(getEvaluateContext(), scopeValue);
    }

    @Override
    protected String createWatchExpression() {
      return null;
    }

    /**
     * Wraps JsScope as an object value with properties representing scope variables.
     */
    class ScopeObjectVariable implements JsObject {
      @Override public JsArray asArray() {
        return null;
      }
      @Override public JsFunction asFunction() {
        return null;
      }
      @Override public String getClassName() {
        return "#Scope";
      }
      @Override public Collection<? extends JsVariable> getProperties() {
        return jsScope.getVariables();
      }
      @Override public Collection<? extends JsVariable> getInternalProperties() {
        return Collections.emptyList();
      }
      @Override public JsVariable getProperty(String name) {
        for (JsVariable var : getProperties()) {
          if (var.getName().equals(name)) {
            return var;
          }
        }
        return null;
      }
      @Override public JsObject asObject() {
        return this;
      }
      @Override public Type getType() {
        return Type.TYPE_OBJECT;
      }
      @Override public String getValueString() {
        return getClassName();
      }
      @Override public String getRefId() {
        return null;
      }
      @Override public RemoteValueMapping getRemoteValueMapping() {
        return null;
      }
      @Override public boolean isTruncated() {
        return false;
      }
      @Override public RelayOk reloadHeavyValue(ReloadBiggerCallback callback,
          SyncCallback syncCallback) {
        return RelaySyncCallback.finish(syncCallback);
      }
    }
  }

  /**
   * A fake variable that represents an exception about to be thrown. Used in a fake
   * ExceptionStackFrame.
   * TODO: consider hiding this public class behind a static factory method.
   */
  public static class NamedHolder extends Variable {
    public static Variable forWithScope(EvaluateContext evaluateContext, JsValue withValue) {
      return new NamedHolder(evaluateContext, "<with>", withValue);
    }

    public static Variable forException(EvaluateContext evaluateContext,
        ExceptionData exceptionData) {
      return new NamedHolder(evaluateContext, "<exception>", exceptionData.getExceptionValue());
    }

    private final String name;
    private final JsValue jsValue;

    private NamedHolder(EvaluateContext evaluateContext, String name, JsValue jsValue) {
      super(evaluateContext);
      this.name = name;
      this.jsValue = jsValue;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
      return jsValue.getType().toString();
    }

    @Override
    protected Value createValue() {
      return Value.create(getEvaluateContext(), jsValue);
    }

    @Override
    protected String createWatchExpression() {
      return null;
    }
  }


  private final AtomicReference<Value> valueRef = new AtomicReference<Value>(null);

  public Variable(EvaluateContext evaluateContext) {
    super(evaluateContext);
  }

  public abstract String getName();

  public abstract String getReferenceTypeName() throws DebugException;

  public Value getValue() {
    Value result = valueRef.get();
    if (result != null) {
      return result;
    }
    // Only set a value if it hasn't be set already (by a concurrent thread).
    valueRef.compareAndSet(null, createValue());
    return valueRef.get();
  }

  protected abstract Value createValue();

  public boolean hasValueChanged() throws DebugException {
    return false;
  }

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
}
