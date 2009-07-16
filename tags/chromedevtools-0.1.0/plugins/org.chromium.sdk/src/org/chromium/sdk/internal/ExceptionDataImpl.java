// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JsObject;

/**
 * An immutable implementation of the ExceptionData interface.
 */
public class ExceptionDataImpl implements ExceptionData {

  private final DebugContextImpl context;
  private final String sourceText;
  private final ValueMirror mirror;
  private final boolean isUncaught;
  private final String exceptionText;
  private JsObject cachedException;

  public ExceptionDataImpl(DebugContextImpl context, ValueMirror mirror, boolean isUncaught,
      String sourceText, String exceptionText) {
    this.context = context;
    this.mirror = mirror;
    this.isUncaught = isUncaught;
    this.sourceText = sourceText;
    this.exceptionText = exceptionText;
  }

  public JsObject getExceptionObject() {
    if (cachedException == null) {
      cachedException = new JsObjectImpl(context.getCallFrames().get(0), mirror.getName(), mirror);
    }
    return cachedException;
  }

  public String getSourceText() {
    return sourceText;
  }

  public boolean isUncaught() {
    return isUncaught;
  }

  public String getExceptionMessage() {
    return exceptionText;
  }

}
