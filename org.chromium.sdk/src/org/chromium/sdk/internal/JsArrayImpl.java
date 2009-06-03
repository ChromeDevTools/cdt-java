// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;

/**
 * A generic implementation of the JsArray interface.
 */
public class JsArrayImpl extends JsObjectImpl implements JsArray {

  private static final SortedMap<Integer, JsVariableImpl> EMPTY_SORTED_MAP =
      Collections.unmodifiableSortedMap(new TreeMap<Integer, JsVariableImpl>());

  /**
   * An indexed sparse array of elements. Keys are indices, values are elements.
   */
  private volatile SortedMap<Integer, JsVariableImpl> indexToElementMap;

  /**
   * This constructor implies lazy resolution of object properties.
   *
   * @param stackFrame this array belongs in
   * @param parentFqn the fully qualified name of this array parent
   * @param valueState the mirror corresponding to this array
   */
  public JsArrayImpl(JsStackFrameImpl stackFrame, String parentFqn, ValueMirror valueState) {
    super(stackFrame, parentFqn, valueState);
  }

  public JsArrayImpl(ValueMirror valueState, JsVariableImpl[] properties) {
    super(valueState, properties);
  }

  private synchronized void ensureElementsMap() {
    if (indexToElementMap != null) {
      return;
    }
    SortedMap<Integer, JsVariableImpl> map =
        new TreeMap<Integer, JsVariableImpl>(new Comparator<Integer>() {
          public int compare(Integer o1, Integer o2) {
            return o1 - o2;
          }
        });

    for (JsVariableImpl prop : getProperties()) {
      String name = prop.getName();
      if (isArrayElementProperty(name)) {
        String indexString = name.substring(1, name.length() - 1);
        if (JsonUtil.isInteger(indexString)) {
          Integer index = Integer.valueOf(indexString);
          map.put(index, prop);
        }
      }
    }
    indexToElementMap = Collections.unmodifiableSortedMap(map);
  }

  @Override
  public JsVariable get(int index) {
    return indexToElementMap.get(index);
  }

  @Override
  public SortedMap<Integer, ? extends JsVariable> toSparseArray() {
    ensureProperties();
    return indexToElementMap;
  }

  @Override
  public int length() {
    int length = 0;
    PropertyReference[] propRefs = getMirror().getProperties();
    for (PropertyReference propRef : propRefs) {
      String name = propRef.getName();
      if (isArrayElementProperty(name)) {
        String indexString = name.substring(1, name.length() - 1);
        try {
          int index = Integer.parseInt(indexString);
          if (index > length) {
            length = index;
          }
        } catch (NumberFormatException e) {
          // not an array element
        }
      }
    }
    return length;
  }

  @Override
  public String toString() {
    SortedMap<Integer, ? extends JsVariable> elements = toSparseArray();
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
  protected void ensureProperties() {
    try {
      if (properties != null) {
        return; // stop recursion
      }
      super.ensureProperties();
      if (!failedResponse) {
        ensureElementsMap();
      }
    } finally {
      if (indexToElementMap == null) {
        indexToElementMap = EMPTY_SORTED_MAP;
      }
    }
  }

  private static boolean isArrayElementProperty(String name) {
    return name.startsWith("[") && name.endsWith("]");
  }

}
