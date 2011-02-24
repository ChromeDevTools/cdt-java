// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import org.chromium.sdk.internal.protocolparser.JsonType;

/**
 * Stores all data for instance of json type.
 * Each implementation of json type interface is a java dynamic proxy, that holds reference
 * to {@link JsonInvocationHandler} which holds reference to this structure. ObjectData points
 * back to dynamic proxy instance in {@link #proxy}.
 */
class ObjectData {

  /**
   * Stores type-specific set of pre-parsed fields.
   */
  private final Object[] fieldArray;

  /**
   * May be JSONObject (in most cases) or any
   * object (for {@link JsonType#subtypesChosenManually()}=true).
   */
  private final Object underlyingObject;
  private final TypeHandler<?> typeHandler;

  /**
   * Holds reference to base type object data (or null).
   */
  private final ObjectData superObjectData;
  private Object proxy = null;

  ObjectData(TypeHandler<?> typeHandler, Object inputObject, int fieldArraySize,
      ObjectData superObjectData) {
    this.superObjectData = superObjectData;
    this.typeHandler = typeHandler;
    this.underlyingObject = inputObject;

    if (fieldArraySize == 0) {
      fieldArray = null;
    } else {
      fieldArray = new Object[fieldArraySize];
    }
  }

  void initProxy(Object proxy) {
    this.proxy = proxy;
  }

  Object[] getFieldArray() {
    return fieldArray;
  }

  Object getUnderlyingObject() {
    return underlyingObject;
  }

  TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  ObjectData getSuperObjectData() {
    return superObjectData;
  }

  Object getProxy() {
    return proxy;
  }

  @Override
  public String toString() {
    return typeHandler.getShortName() + "/" + underlyingObject;
  }
}
