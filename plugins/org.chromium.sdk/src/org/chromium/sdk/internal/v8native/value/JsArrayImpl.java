// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.value;

import static org.chromium.sdk.util.BasicUtil.getSafe;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.util.JavaScriptExpressionBuilder;
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
  public JsVariable get(long index) throws MethodIsBlockingException {
    return getSafe(getPropertyData(true).ensureElementsMap(), index);
  }

  @Override
  public SortedMap<Long, ? extends JsVariable> toSparseArray() throws MethodIsBlockingException {
    return getPropertyData(true).ensureElementsMap();
  }

  @Override
  public long getLength() throws MethodIsBlockingException {
    SortedMap<Long, ?> map = getPropertyData(true).ensureElementsMap();
    if (map.isEmpty()) {
      return 0;
    }
    return map.lastKey() + 1;
  }

  @Override
  public String toString() {
    SortedMap<?, ? extends JsVariable> elements;
    try {
      elements = toSparseArray();
    } catch (MethodIsBlockingException e) {
      return "[JsArray: Exception in retrieving data]";
    }
    StringBuilder result = new StringBuilder();
    result.append("[JsArray: length=").append(elements.size());
    for (Map.Entry<?, ? extends JsVariable> entry : elements.entrySet()) {
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
    private SortedMap<Long, JsVariableBase> indexToElementMap = null;

    ArrayPropertyData(BasicPropertyData basicPropertyData) {
      this.basicPropertyData = basicPropertyData;
    }

    BasicPropertyData getBasicPropertyData() {
      return basicPropertyData;
    }

    private synchronized SortedMap<Long, JsVariableBase> ensureElementsMap() {
      if (indexToElementMap == null) {
        SortedMap<Long, JsVariableBase> map = new TreeMap<Long, JsVariableBase>();

        for (JsVariableBase prop : basicPropertyData.getPropertyList()) {
          Object name = prop.getRawNameAsObject();
          Long key;
          if (name instanceof Long) {
            Long index = (Long) name;
            if (!JavaScriptExpressionBuilder.checkArrayIndexValue(index)) {
              continue;
            }
            key = index;
          } else {
            key = JavaScriptExpressionBuilder.parsePropertyNameAsArrayIndex(name.toString());
            if (key == null) {
              continue;
            }
          }
          map.put(key, prop);
        }
        // We make map synchronized for such methods as entrySet, that are not thread-safe.
        indexToElementMap =
            Collections.unmodifiableSortedMap(Collections.synchronizedSortedMap(map));
      }
      return indexToElementMap;
    }
  }
}
