// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
      cachedException = JsVariableImpl.createValue(context.getValueLoader(), mirror, "<exception>");
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
