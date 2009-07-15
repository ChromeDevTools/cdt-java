// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.JsValue;

/**
 * A base class that represents a JavaScript VM variable value (compound values
 * are represented by subclasses.)
 */
public class JsValueImpl implements JsValue {

  /** The value data as reported by the JavaScript VM. */
  private final ValueMirror valueData;

  public JsValueImpl(ValueMirror valueData) {
    this.valueData = valueData;
  }

  public Type getType() {
    return valueData.getType();
  }

  public String getValueString() {
    return valueData.toString();
  }

  public JsObjectImpl asObject() {
    return null;
  }

  public ValueMirror getMirror() {
    return this.valueData;
  }

  @Override
  public String toString() {
    return String.format("[JsValue: type=%s,value=%s]", getType(), getValueString());
  }
}
