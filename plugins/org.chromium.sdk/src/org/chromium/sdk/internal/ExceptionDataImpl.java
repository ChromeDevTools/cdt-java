// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;

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
  private JsValueImpl cachedException;

  public ExceptionDataImpl(InternalContext context, ValueMirror mirror, String name,
      boolean isUncaught, String sourceText, String exceptionText) {
    this.context = context;
    this.mirror = mirror;
    this.name = name;
    this.isUncaught = isUncaught;
    this.sourceText = sourceText;
    this.exceptionText = exceptionText;
  }

  @Deprecated
  @Override
  public JsObject getExceptionObject() {
    return getExceptionValue().asObject();
  }

  @Override
  public JsValue getExceptionValue() {
    if (cachedException == null) {
      cachedException = JsVariableImpl.createValue(context, mirror, "<exception>");
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
