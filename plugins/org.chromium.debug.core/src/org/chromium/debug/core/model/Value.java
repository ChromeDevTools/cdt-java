// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.JsValueStringifier;
import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * A generic (non-array) implementation of IValue using a JsValue instance.
 */
public class Value extends DebugElementImpl implements IValue {

  private static final IVariable[] EMPTY_VARIABLES = new IVariable[0];

  private final JsValue value;

  private IVariable[] variables;

  public static Value create(DebugTargetImpl debugTarget, JsValue value) {
    if (JsValue.Type.TYPE_ARRAY == value.getType()) {
      return new ArrayValue(debugTarget, (JsArray) value);
    }
    return new Value(debugTarget, value);
  }

  Value(DebugTargetImpl debugTarget, JsValue value) {
    super(debugTarget);
    this.value = value;
  }

  public String getReferenceTypeName() throws DebugException {
    return value.getType().toString();
  }

  public String getValueString() throws DebugException {
    String valueText = JsValueStringifier.toVisibleString(value);
    if (value.asObject() != null) {
      String ref = value.asObject().getRefId();
      if (ref != null) {
        valueText = valueText + "  (id=" + ref + ")";
      }
    }
    return valueText;
  }

  public IVariable[] getVariables() throws DebugException {
    try {
      if (variables == null) {
        if (value.asObject() != null) {
          variables = StackFrame.wrapVariables(getDebugTarget(), value.asObject().getProperties(),
              value.asObject().getInternalProperties());
        } else {
          variables = EMPTY_VARIABLES;
        }
      }
      return variables;
    } catch (RuntimeException e) {
      // We shouldn't throw RuntimeException from here, because calling
      // ElementContentProvider#update will forget to call update.done().
      throw new DebugException(new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
          "Failed to read variables", e)); //$NON-NLS-1$
    }
  }

  public boolean hasVariables() throws DebugException {
    return value.asObject() != null;
  }

  public boolean isAllocated() throws DebugException {
    return false;
  }

  public JsValue getJsValue() {
    return value;
  }

}
