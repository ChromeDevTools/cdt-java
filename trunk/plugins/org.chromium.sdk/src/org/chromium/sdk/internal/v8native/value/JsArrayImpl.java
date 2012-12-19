// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * A generic implementation of the JsArray interface.
 */
class JsArrayImpl extends JsObjectBase<JsArrayImpl.ArrayPropertyData> implements JsArray {

  /**
   * This constructor implies lazy resolution of object properties.
   *
   * @param context this array belongs in
   * @param valueState the mirror corresponding to this array
   */
  JsArrayImpl(ValueLoader valueLoader, ValueMirror valueState) {
    super(valueLoader, valueState);
  }

  @Override
  public JsVariable get(int index) throws MethodIsBlockingException {
    return getPropertyData(true).ensureElementsMap().get(index);
  }

  @Override
  public SortedMap<Integer, ? extends JsVariable> toSparseArray() throws MethodIsBlockingException {
    return getPropertyData(true).ensureElementsMap();
  }

  @Override
  public int length() throws MethodIsBlockingException {
    // TODO(peter.rybin) optimize it: either read "length" from remote or count PropertyReference
    // rather than JsVariableImpl
    int lastIndex = -1;
    Collection<JsVariableImpl> properties = getProperties();
    // TODO(peter.rybin): rename propRefs
    for (JsVariableImpl prop : properties) {
      Object name = prop.getRawNameAsObject();
      if (name instanceof Number == false) {
        continue;
      }
      Number index = (Number) name;
      int intIndex = index.intValue();
      if (intIndex > lastIndex) {
        lastIndex = intIndex;
      }
    }
    return lastIndex + 1;
  }

  @Override
  public String toString() {
    SortedMap<Integer, ? extends JsVariable> elements;
    try {
      elements = toSparseArray();
    } catch (MethodIsBlockingException e) {
      return "[JsArray: Exception in retrieving data]";
    }
    StringBuilder result = new StringBuilder();
    result.append("[JsArray: length=").append(elements.size());
    for (Map.Entry<Integer, ? extends JsVariable> entry : elements.entrySet()) {
      result.append(',').append(entry.getKey()).append('=').append(entry.getValue());
    }
    result.append(']');
    return result.toString();
  }

  @Override
  public JsArrayImpl asArray() {
    return this;
  }

  @Override
  public JsFunction asFunction() {
    return null;
  }

  @Override
  protected ArrayPropertyData wrapBasicData(BasicPropertyData basicPropertyData) {
    return new ArrayPropertyData(basicPropertyData);
  }

  @Override
  protected BasicPropertyData unwrapBasicData(ArrayPropertyData wrappedBasicData) {
    return wrappedBasicData.getBasicPropertyData();
  }

  /**
   * Wraps basic property data and contains lazy-initialized field indexToElementMap.
   * This is needed because {@link JsObjectBase} will dispose of it when caches
   * become reset.
   */
  static class ArrayPropertyData {
    private final BasicPropertyData basicPropertyData;

    /**
     * An indexed sparse array of elements. Keys are indices, values are elements.
     */
    private SortedMap<Integer, JsVariableImpl> indexToElementMap = null;

    ArrayPropertyData(BasicPropertyData basicPropertyData) {
      this.basicPropertyData = basicPropertyData;
    }

    BasicPropertyData getBasicPropertyData() {
      return basicPropertyData;
    }

    private synchronized SortedMap<Integer, JsVariableImpl> ensureElementsMap() {
      if (indexToElementMap == null) {
        SortedMap<Integer, JsVariableImpl> map = new TreeMap<Integer, JsVariableImpl>();

        for (JsVariableImpl prop : basicPropertyData.getPropertyList()) {
          Object name = prop.getRawNameAsObject();
          if (name instanceof Number == false) {
            continue;
          }
          Number index = (Number) name;
          map.put(index.intValue(), prop);
        }
        // We make map synchronized for such methods as entrySet, that are not thread-safe.
        indexToElementMap =
            Collections.unmodifiableSortedMap(Collections.synchronizedSortedMap(map));
      }
      return indexToElementMap;
    }
  }
}
