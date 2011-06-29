// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.MethodIsBlockingException;

/**
 * A generic implementation of the JsObject interface.
 */
public class JsObjectImpl extends JsValueImpl implements JsObject {

  private final InternalContext context;

  /**
   * Fully qualified name of variable holding this object.
   */
  private final String variableFqn;

  /**
   * This constructor implies the lazy resolution of object properties.
   *
   * @param context where this instance belongs in
   * @param variableFqn the fully qualified name of the variable holding this object
   * @param valueState the value data from the JS VM
   */
  JsObjectImpl(InternalContext context, String variableFqn, ValueMirror valueState) {
    super(valueState);
    this.context = context;
    this.variableFqn = variableFqn;
  }

  public Collection<JsVariableImpl> getProperties() throws MethodIsBlockingException {
    return subproperties.getPropertiesLazily();
  }

  public Collection<JsVariableImpl> getInternalProperties() throws MethodIsBlockingException {
    return internalProperties.getPropertiesLazily();
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

  public static int parseRefId(String value) {
    return Integer.parseInt(value);
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

  public JsFunction asFunction() {
    return null;
  }

  public JsVariable getProperty(String name) {
    return subproperties.getProperty(name);
  }

  public String getClassName() {
    return getMirror().getClassName();
  }

  protected InternalContext getInternalContext() {
    return context;
  }

  Subproperties getSubpropertiesHelper() {
    return subproperties;
  }

  protected SubpropertiesMirror getSubpropertiesMirror() {
    return context.getValueLoader().loadSubpropertiesInMirror(getMirror()).getSubpropertiesMirror();
  }

  abstract class Subproperties {
    private List<JsVariableImpl> properties = null;
    private Map<String, JsVariableImpl> propertyMap = null;

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


    private List<JsVariableImpl> createPropertiesFromMirror(List<ValueMirror> mirrorProperties,
        List<? extends PropertyReference> propertyRefs) throws MethodIsBlockingException {
      // TODO(peter.rybin) Maybe assert that context is valid here

      List<JsVariableImpl> result = new ArrayList<JsVariableImpl>(mirrorProperties.size());
      for (int i = 0; i < mirrorProperties.size(); i++) {
        ValueMirror mirror = mirrorProperties.get(i);
        Object varName = propertyRefs.get(i).getName();
        String fqn = getFullyQualifiedName(varName);
        if (fqn == null) {
          continue;
        }
        String decoratedName = JsVariableImpl.NameDecorator.decorateVarName(varName);
        result.add(new JsVariableImpl(context, mirror, varName, decoratedName, fqn));
      }
      return result;
    }

    private String getFullyQualifiedName(Object propName) {
      if (variableFqn == null) {
        return null;
      }
      if (propName instanceof String) {
        String propNameStr = (String) propName;
        if (propNameStr.startsWith(".")) {
          // ".arguments" is not legal
          return null;
        }
      }
      return variableFqn + JsVariableImpl.NameDecorator.buildAccessSuffix(propName);
    }

    JsVariableImpl getProperty(String propertyName) {
      return ensurePropertyMap().get(propertyName);
    }

    List<JsVariableImpl> getPropertiesLazily() throws MethodIsBlockingException {
      synchronized (this) {
        if (properties == null) {
          List<? extends PropertyReference> propertyRefs =
              getPropertyRefs(getSubpropertiesMirror());
          ValueLoader valueLoader = context.getValueLoader();
          List<ValueMirror> subMirrors = valueLoader.getOrLoadValueFromRefs(propertyRefs);

          List<JsVariableImpl> wrappedProperties = createPropertiesFromMirror(subMirrors,
              propertyRefs);
          properties = Collections.unmodifiableList(wrappedProperties);
        }
        return properties;
      }
    }

    abstract List<? extends PropertyReference> getPropertyRefs(
        SubpropertiesMirror subpropertiesMirror);
  }

  private final Subproperties subproperties = new Subproperties() {
    @Override
    List<? extends PropertyReference> getPropertyRefs(SubpropertiesMirror subpropertiesMirror) {
      return subpropertiesMirror.getProperties();
    }
  };

  private final Subproperties internalProperties = new Subproperties() {
    @Override
    List<? extends PropertyReference> getPropertyRefs(SubpropertiesMirror subpropertiesMirror) {
      return subpropertiesMirror.getInternalProperties();
    }
  };
}
