// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JsVariable;

/**
 * An immutable implementation of the ExceptionData interface.
 */
public class ExceptionDataImpl implements ExceptionData {

  private final DebugContextImpl context;
  private final String sourceText;
  private final ValueMirror mirror;
  private final boolean isUncaught;
  private final String exceptionText;
  private JsVariable cachedException;

  public ExceptionDataImpl(DebugContextImpl context, ValueMirror mirror, boolean isUncaught,
      String sourceText, String exceptionText) {
    this.context = context;
    this.mirror = mirror;
    this.isUncaught = isUncaught;
    this.sourceText = sourceText;
    this.exceptionText = exceptionText;
  }

  public JsVariable getException() {
    if (cachedException == null) {
      cachedException = new JsVariableImpl(context.getStackFrames()[0], mirror);
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
  public String getExceptionText() {
    return exceptionText;
  }

}
