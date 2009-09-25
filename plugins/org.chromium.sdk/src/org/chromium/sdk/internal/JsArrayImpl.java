// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * A generic implementation of the JsArray interface.
 */
public class JsArrayImpl extends JsObjectImpl implements JsArray {

  /**
   * An indexed sparse array of elements. Keys are indices, values are elements.
   */
  private SortedMap<Integer, JsVariableImpl> indexToElementMap;

  /**
   * This constructor implies lazy resolution of object properties.
   *
   * @param callFrame this array belongs in
   * @param parentFqn the fully qualified name of this array parent
   * @param valueState the mirror corresponding to this array
   */
  public JsArrayImpl(CallFrameImpl callFrame, String parentFqn, ValueMirror valueState) {
    super(callFrame, parentFqn, valueState);
  }

  private synchronized void ensureElementsMap() throws MethodIsBlockingException {
    if (indexToElementMap != null) {
      return;
    }
    SortedMap<Integer, JsVariableImpl> map =
      // TODO(peter.rybin): do we need this comparator at all?
        new TreeMap<Integer, JsVariableImpl>(new Comparator<Integer>() {
          public int compare(Integer o1, Integer o2) {
            return o1 - o2;
          }
        });

    for (JsVariableImpl prop : getProperties()) {
      String name = prop.getRawName();
      Integer index = getAsArrayIndex(name);
      if (index == null) {
        continue;
      }
      map.put(index, prop);
    }
    indexToElementMap = Collections.unmodifiableSortedMap(map);
  }

  public JsVariable get(int index) throws MethodIsBlockingException {
    ensureElementsMap();
    return indexToElementMap.get(index);
  }

  public SortedMap<Integer, ? extends JsVariable> toSparseArray() throws MethodIsBlockingException {
    ensureElementsMap();
    return indexToElementMap;
  }

  public int length() {
    // TODO(peter.rybin) optimize it: either read "length" from remote or count PropertyReference
    // rather than JsVariableImpl
    int lastIndex = -1;
    List<JsVariableImpl> properties = getPropertiesLazily();
    // TODO(peter.rybin): rename propRefs
    for (JsVariableImpl prop : properties) {
      String name = prop.getRawName();
      Integer index = getAsArrayIndex(name);
      if (index == null) {
        continue;
      }
      if (index > lastIndex) {
        lastIndex = index;
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
  protected JsVariableImpl.NameDecorator getChildPropertyNameDecorator() {
    return ARRAY_ELEMENT_DECORATOR;
  }

  /**
   * @return integer representation of the index or null if it is not an integer
   */
  static Integer getAsArrayIndex(String varName) {
    if (!JsonUtil.isInteger(varName)) {
      return null;
    }
    try {
      int index = Integer.parseInt(varName);
      return index;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private final static JsVariableImpl.NameDecorator ARRAY_ELEMENT_DECORATOR =
      new JsVariableImpl.NameDecorator() {
    @Override
    String decorateVarName(String rawName) {
      Integer index = getAsArrayIndex(rawName);
      if (index == null) {
        return rawName;
      }
      // Fix array element indices
      return OPEN_BRACKET + rawName + CLOSE_BRACKET;
    }
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
  };
}
