// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.chromium.debug.core.tools.v8.model.mirror.ValueMirror;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;

/**
 * Represents a Chromium V8 VM variable value.
 */
public class Value extends DebugElementImpl implements IValue {

  private static final Variable[] EMPTY_VARIABLES = new Variable[0];

  private ValueMirror valueState;

  private Variable[] variables;

  public Value(V8DebuggerToolHandler handler, ValueMirror valueState) {
    super(handler);
    this.valueState = valueState;
  }

  public Value(V8DebuggerToolHandler handler, ValueMirror valueState,
      Variable[] vars) {
    super(handler);
    this.valueState = valueState;
    this.variables = vars;
  }

  public String getReferenceTypeName() throws DebugException {
    return valueState.getType().jsonType;
  }

  public String getValueString() throws DebugException {
    return valueState.toString();
  }

  public boolean isAllocated() throws DebugException {
    return true;
  }

  public Variable[] getVariables() throws DebugException {
    return hasVariables() ? variables : EMPTY_VARIABLES;
  }

  public boolean hasVariables() throws DebugException {
    return variables != null && variables.length > 0;
  }

}
