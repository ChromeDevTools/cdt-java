// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.MethodIsBlockingException;

/**
 * A generic implementation of the JsArray interface.
 */
class JsArrayImpl extends JsObjectBase implements JsArray {

  /**
   * An indexed sparse array of elements. Keys are indices, values are elements.
   */
  private SortedMap<Integer, JsVariableImpl> indexToElementMap;

  /**
   * This constructor implies lazy resolution of object properties.
   *
   * @param context this array belongs in
   * @param variableFqn the fully qualified name of the variable holding this array
   * @param valueState the mirror corresponding to this array
   */
  JsArrayImpl(InternalContext context, String variableFqn, ValueMirror valueState) {
    super(context, variableFqn, valueState);
  }

  private synchronized void ensureElementsMap() throws MethodIsBlockingException {
    if (indexToElementMap != null) {
      return;
    }
    SortedMap<Integer, JsVariableImpl> map =
      // TODO(peter.rybin): do we need this comparator at all?
        new TreeMap<Integer, JsVariableImpl>(new Comparator<Integer>() {
          @Override public int compare(Integer o1, Integer o2) {
            return o1 - o2;
          }
        });

    for (JsVariableImpl prop : getProperties()) {
      Object name = prop.getRawNameAsObject();
      if (name instanceof Number == false) {
        continue;
      }
      Number index = (Number) name;
      map.put(index.intValue(), prop);
    }
    indexToElementMap = Collections.unmodifiableSortedMap(map);
  }

  @Override
  public JsVariable get(int index) throws MethodIsBlockingException {
    ensureElementsMap();
    return indexToElementMap.get(index);
  }

  @Override
  public SortedMap<Integer, ? extends JsVariable> toSparseArray() throws MethodIsBlockingException {
    ensureElementsMap();
    return indexToElementMap;
  }

  @Override
  public int length() {
    // TODO(peter.rybin) optimize it: either read "length" from remote or count PropertyReference
    // rather than JsVariableImpl
    int lastIndex = -1;
    List<JsVariableImpl> properties = getSubpropertiesHelper().getPropertiesLazily();
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
}
