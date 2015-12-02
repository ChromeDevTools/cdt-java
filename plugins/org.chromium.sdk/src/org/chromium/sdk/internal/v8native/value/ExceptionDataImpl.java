// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.internal.v8native.InternalContext;

/**
 * An immutable implementation of the ExceptionData interface.
 */
public class ExceptionDataImpl implements ExceptionData {

  private final InternalContext context;
  private final String sourceText;
  private final ValueMirror mirror;
  private final String name;
  private final boolean isUncaught;
  private final String exceptionText;
  private JsValueBase cachedException;

  public ExceptionDataImpl(InternalContext context, ValueMirror mirror, String name,
      boolean isUncaught, String sourceText, String exceptionText) {
    this.context = context;
    this.mirror = mirror;
    this.name = name;
    this.isUncaught = isUncaught;
    this.sourceText = sourceText;
    this.exceptionText = exceptionText;
  }

  @Override
  public JsValue getExceptionValue() {
    if (cachedException == null) {
      // TODO: make it thread-safe.
      cachedException = JsVariableBase.createValue(context.getValueLoader(), mirror);
    }
    return cachedException;
  }

  @Override
  public String getSourceText() {
    return sourceText;
  }

  @Override
  public boolean isUncaught() {
    return isUncaught;
  }

  @Override
  public String getExceptionMessage() {
    return exceptionText;
  }
}
