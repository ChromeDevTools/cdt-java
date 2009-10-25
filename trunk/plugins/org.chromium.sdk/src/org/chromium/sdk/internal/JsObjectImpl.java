// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * A generic implementation of the JsObject interface.
 */
public class JsObjectImpl extends JsValueImpl implements JsObject {

  private List<JsVariableImpl> properties = null;

  private final CallFrameImpl callFrame;

  private final String parentFqn;

  private Map<String, JsVariableImpl> propertyMap = null;

  /**
   * A lock for the properties and propertyMap fields access/modification.
   */
  private final Object propertyLock = new Object();

  /**
   * This constructor implies the lazy resolution of object properties.
   *
   * @param callFrame where this instance belongs in
   * @param parentFqn the fully qualified name of the object parent
   * @param valueState the value data from the JS VM
   */
  public JsObjectImpl(CallFrameImpl callFrame, String parentFqn, ValueMirror valueState) {
    super(valueState);
    this.callFrame = callFrame;
    this.parentFqn = parentFqn;
  }

  public Collection<JsVariableImpl> getProperties() throws MethodIsBlockingException {
    return getPropertiesLazily();
  }

  /**
   * Calls to this method must be synchronized on propertyLock.
   */
  private Map<String, JsVariableImpl> ensurePropertyMap() {
    if (propertyMap == null) {
      List<JsVariableImpl> propertiesList = getPropertiesLazily();
      Map<String, JsVariableImpl> map =
          new HashMap<String, JsVariableImpl>(propertiesList.size() * 2, 0.75f);
      for (JsVariableImpl prop : propertiesList) {
        map.put(prop.getName(), prop);
      }
      propertyMap = Collections.unmodifiableMap(map);
    }
    return propertyMap;
  }

  public List<JsVariableImpl> getPropertiesLazily() throws MethodIsBlockingException {
    synchronized (propertyLock) {
      if (properties == null) {

        ValueLoader valueLoader = callFrame.getInternalContext().getValueLoader();

        List<? extends PropertyReference> propertyRefs =
            valueLoader.loadSubpropertiesInMirror(getMirror())
            .getSubpropertiesMirror().getProperties();
        List<ValueMirror> subMirrors = valueLoader.getOrLoadValueFromRefs(propertyRefs);

        List<JsVariableImpl> wrappedProperties = createPropertiesFromMirror(subMirrors,
            propertyRefs);
        properties = Collections.unmodifiableList(wrappedProperties);
      }
      return properties;
    }
  }

  public String getRefId() {
    int ref = getMirror().getRef();
    if (ref < 0) {
      // Negative handle means that it's transient. We don't expose it.
      return null;
    } else {
      return String.valueOf(ref);
    }
  }


  private List<JsVariableImpl> createPropertiesFromMirror(List<ValueMirror> mirrorProperties,
      List<? extends PropertyReference> propertyRefs) throws MethodIsBlockingException {
    // TODO(peter.rybin) Maybe assert that context is valid here

    List<JsVariableImpl> result = new ArrayList<JsVariableImpl>(mirrorProperties.size());
    for (int i = 0; i < mirrorProperties.size(); i++) {
      ValueMirror mirror = mirrorProperties.get(i);
      String varName = propertyRefs.get(i).getName();
      String fqn = getFullyQualifiedName(varName);
      if (fqn == null) {
        continue;
      }
      result.add(new JsVariableImpl(callFrame, mirror, varName, fqn,
          getChildPropertyNameDecorator()));
    }
    return result;
  }

  private String getFullyQualifiedName(String propName) {
    if (propName.startsWith(".")) {
      // ".arguments" is not legal
      return null;
    }
    return parentFqn + getChildPropertyNameDecorator().buildAccessSuffix(propName);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("[JsObject: type=").append(getType());
    try {
      for (JsVariable prop : getProperties()) {
        result.append(',').append(prop);
      }
    } catch (MethodIsBlockingException e) {
      return "[JsObject: Exception in retrieving data]";
    }
    result.append(']');
    return result.toString();
  }

  @Override
  public JsObjectImpl asObject() {
    return this;
  }

  public JsArrayImpl asArray() {
    return null;
  }

  public JsVariable getProperty(String name) {
    return ensurePropertyMap().get(name);
  }

  public String getClassName() {
    return getMirror().getClassName();
  }

  protected JsVariableImpl.NameDecorator getChildPropertyNameDecorator() {
    return JsVariableImpl.NameDecorator.NOOP;
  }
}
