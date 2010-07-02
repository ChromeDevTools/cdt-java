// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter;

/**
 * An IVariable implementation over a JsVariable instance.
 */
public class Variable extends DebugElementImpl implements IVariable {

  private final JsVariable variable;

  /**
   * Specifies whether this variable is internal property (__proto__ etc).
   * TODO(peter.rybin): use it in UI.
   */
  private final boolean isInternalProperty;

  public Variable(DebugTargetImpl debugTarget, JsVariable variable, boolean isInternalProperty) {
    super(debugTarget);
    this.variable = variable;
    this.isInternalProperty = isInternalProperty;
  }

  public String getName() {
    return variable.getName();
  }

  public String getReferenceTypeName() throws DebugException {
    return variable.getValue().getType().toString();
  }

  public IValue getValue() throws DebugException {
    JsValue value = variable.isReadable()
        ? variable.getValue()
        : null;
    if (value == null) {
      return null;
    }
    return wrapValue(value);
  }

  public boolean hasValueChanged() throws DebugException {
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter) {
    if (IWatchExpressionFactoryAdapter.class == adapter) {
      return new IWatchExpressionFactoryAdapter() {
        public String createWatchExpression(IVariable variable) throws CoreException {
          String expression = ((Variable) variable).getJsVariable().getFullyQualifiedName();
          if (expression == null) {
            expression = variable.getName();
          }
          return expression;
        }
      };
    }
    return super.getAdapter(adapter);
  }

  public void setValue(String expression) throws DebugException {
    variable.setValue(expression, null);
  }

  public void setValue(IValue value) throws DebugException {
    variable.setValue(((Value) value).getJsValue().getValueString(), null);
  }

  public boolean supportsValueModification() {
    return false; // TODO(apavlov): fix once V8 supports it
  }

  public boolean verifyValue(IValue value) throws DebugException {
    return verifyValue(value.getValueString());
  }

  public boolean verifyValue(String expression) {
    switch (variable.getValue().getType()) {
      case TYPE_NUMBER:
        return ChromiumDebugPluginUtil.isInteger(expression);
      default:
        return true;
    }
  }

  public boolean verifyValue(JsValue value) {
    return verifyValue(value.getValueString());
  }

  private IValue wrapValue(JsValue value) {
    return Value.create(getDebugTarget(), value);
  }

  public JsVariable getJsVariable() {
    return variable;
  }

}
