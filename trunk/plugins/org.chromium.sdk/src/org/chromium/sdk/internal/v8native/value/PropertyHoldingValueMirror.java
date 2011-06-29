// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

/**
 * A representation of a properties data of a value in the remote JavaScript VM.
 * Must be immutable. Conceptually it always corresponds to a {@link ValueMirror}
 * and in a way should behave like dynamic subclass of ValueMirror.
 */
public class PropertyHoldingValueMirror {
  private final ValueMirror valueMirror;
  private final SubpropertiesMirror subpropertiesMirror;

  PropertyHoldingValueMirror(ValueMirror valueMirror) {
    this.valueMirror = valueMirror;
    this.subpropertiesMirror = SubpropertiesMirror.EMPTY;
  }

  PropertyHoldingValueMirror(ValueMirror valueMirror, SubpropertiesMirror subpropertiesMirror) {
    this.valueMirror = valueMirror;
    this.subpropertiesMirror = subpropertiesMirror;
  }

  public ValueMirror getValueMirror() {
    return valueMirror;
  }

  public SubpropertiesMirror getSubpropertiesMirror() {
    return subpropertiesMirror;
  }
}
