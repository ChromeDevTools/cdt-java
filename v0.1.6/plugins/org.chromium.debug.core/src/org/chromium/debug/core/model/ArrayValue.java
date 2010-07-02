// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collections;
import java.util.Set;

import org.chromium.sdk.JsArray;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * An IIndexedValue implementation for an array element range using a JsArray
 * instance.
 */
public class ArrayValue extends Value implements IIndexedValue {

  private final IVariable[] elements;

  public ArrayValue(DebugTargetImpl debugTarget, JsArray array) {
    super(debugTarget, array);
    this.elements = createElements();
  }

  private IVariable[] createElements() {
    JsArray jsArray = (JsArray) getJsValue();
    return StackFrame.wrapVariables(getDebugTarget(), jsArray.getProperties(),
        ARRAY_HIDDEN_PROPERTY_NAMES,
        // Do not show internal properties for arrays (this may be an option).
        null);
  }

  public int getInitialOffset() {
    return 0;
  }

  public int getSize() throws DebugException {
    return elements.length;
  }

  public IVariable getVariable(int offset) throws DebugException {
    return elements[offset];
  }

  public IVariable[] getVariables(int offset, int length) throws DebugException {
    IVariable[] result = new IVariable[length];
    System.arraycopy(elements, offset, result, 0, length);
    return result;
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    return elements;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return elements.length > 0;
  }

  private static final Set<String> ARRAY_HIDDEN_PROPERTY_NAMES = Collections.singleton("length");
}
