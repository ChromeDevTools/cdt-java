// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;


/**
 * A named property reference.
 */
public class PropertyReference {
  private final Object name;

  private final DataWithRef smthWithRef;

  /**
   * @param propertyName the name of the property
   * @param valueObject a JSON descriptor of the property
   */
  public PropertyReference(Object propertyName, DataWithRef smthWithRef) {
    this.name = propertyName;
    this.smthWithRef = smthWithRef;
  }

  public long getRef() {
    return smthWithRef.ref();
  }

  public Object getName() {
    return name;
  }

  public DataWithRef getValueObject() {
    return smthWithRef;
  }
}