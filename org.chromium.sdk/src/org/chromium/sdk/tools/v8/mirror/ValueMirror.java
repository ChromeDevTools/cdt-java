// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.model.mirror;

import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.v8.Protocol;

/**
 * Represents Chromium-based data for a Value.
 */
public class ValueMirror {
  public enum Type {
    JS_UNDEFINED(Protocol.TYPE_UNDEFINED),
    JS_NULL(Protocol.TYPE_NULL),
    JS_BOOLEAN(Protocol.TYPE_BOOLEAN),
    JS_NUMBER(Protocol.TYPE_NUMBER),
    JS_STRING(Protocol.TYPE_STRING),
    JS_OBJECT(Protocol.TYPE_OBJECT),
    JS_ARRAY(Protocol.TYPE_OBJECT_ARRAY),
    JS_DATE(Protocol.TYPE_DATE),
    JS_FUNCTION(Protocol.TYPE_FUNCTION),
    ;

    public String jsonType;

    private static Map<String, Type> jsonTypeToEnum =
        new HashMap<String, Type>();

    static {
      for (Type type : values()) {
        jsonTypeToEnum.put(type.jsonType, type);
      }
    }

    Type(String jsonType) {
      this.jsonType = jsonType;
    }

    public static Type fromJsonTypeAndClassName(String jsonType,
        String className) {
      if (jsonType == null) {
        return null;
      }
      if (Protocol.CLASSNAME_DATE.equals(className)) {
        // hack to use the JS_DATE type even though its type in V8 is "object"
        return JS_DATE;
      } else if (Protocol.CLASSNAME_ARRAY.equals(className)) {
        // hack to use the JS_ARRAY type even though its type in V8 is "object"
        return JS_ARRAY;
      }
      return jsonTypeToEnum.get(jsonType);
    }
  }

  public static class PropertyReference {
    private final int ref;

    private final String name;

    public PropertyReference(int refId, String propName) {
      this.ref = refId;
      this.name = propName;
    }

    public int getRef() {
      return ref;
    }

    public String getName() {
      return name;
    }
  }

  private Type type;

  private final String name;

  private String value;

  private PropertyReference[] properties = null;

  private int ref = -1;

  private int parentRef = -1;

  public ValueMirror(String varName, int v) {
    this.name = varName;
    this.type = Type.JS_NUMBER;
    this.value = Integer.toString(v);
  }

  public ValueMirror(String varName, String v) {
    this.type = Type.JS_STRING;
    this.name = varName;
    this.value = v;
  }

  public ValueMirror(String varName, int refID, int parentRefID) {
    this.type = Type.JS_OBJECT;
    this.name = varName;
    this.ref = refID;
    this.parentRef = parentRefID;
  }

  public ValueMirror(String varName, int refID, PropertyReference[] props,
      String className) {
    this.type = getJsType(className);
    this.name = varName;
    this.ref = refID;
    this.properties = props;
  }

  public void setProperties(PropertyReference[] props) {
    this.properties = props;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public PropertyReference[] getProperties() {
    return properties;
  }

  public int getParentRef() {
    return parentRef;
  }

  public int getRef() {
    return ref;
  }

  /**
   * @return the type as known by Eclipse.
   */
  public String getTypeAsString() {
    // TODO(apavlov): refactor
    switch (type) {
      case JS_NUMBER:
        return Protocol.TYPE_NUMBER;
      case JS_OBJECT:
        return Protocol.TYPE_OBJECT;
      case JS_ARRAY:
        return Protocol.TYPE_OBJECT_ARRAY;
      case JS_FUNCTION:
        return Protocol.TYPE_FUNCTION;
      case JS_STRING:
      default:
        return "text"; //$NON-NLS-1$
    }
  }

  public void setValue(String val) {
    switch (type) {
      case JS_UNDEFINED:
      case JS_NULL:
      case JS_BOOLEAN:
      case JS_DATE:
      case JS_STRING:
      case JS_NUMBER:
        value = val;
        break;
      case JS_OBJECT:
      case JS_ARRAY:
      case JS_FUNCTION:
      default:
        ChromiumDebugPlugin.logWarning("setValue not legal for this type"); //$NON-NLS-1$
    }
  }

  public void setType(Type type) {
    this.type = type;
  }

  @Override
  public String toString() {
    switch (type) {
      case JS_UNDEFINED:
      case JS_NULL:
      case JS_DATE:
      case JS_STRING:
      case JS_NUMBER:
      case JS_BOOLEAN:
        return value == null ? "" : value; //$NON-NLS-1$
      case JS_OBJECT:
        return "[Object]"; //$NON-NLS-1$
      case JS_ARRAY:
        return "[Array]"; //$NON-NLS-1$
      case JS_FUNCTION:
        return "[Function]"; //$NON-NLS-1$
      default:
        return ""; //$NON-NLS-1$
    }
  }

  private static Type getJsType(String className) {
    return Protocol.CLASSNAME_ARRAY.equals(className)
        ? Type.JS_ARRAY
        : Protocol.CLASSNAME_DATE.equals(className)
            ? Type.JS_DATE
            : Type.JS_OBJECT;
  }
}
