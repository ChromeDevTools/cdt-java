// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.JsDataType;

/**
 * A representation of a datum (value) in the remote Javascript VM.
 */
public class ValueMirror {

  /**
   * Class name of a generic object.
   */
  private static final String OBJECT_CLASSNAME = "Object";

  /**
   * A named property reference.
   */
  static class PropertyReference {
    private final int ref;

    private final String name;

    PropertyReference(int refId, String propName) {
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

  private final String name;

  private final int ref;

  private JsDataType type;

  private String value;

  private PropertyReference[] properties = null;

  private String className;

  public ValueMirror(String varName, String value) {
    this(varName, value, JsDataType.TYPE_STRING);
  }

  public ValueMirror(String varName, String value, JsDataType type) {
    this.type = type;
    this.name = varName;
    this.value = value;
    this.ref = -1;
    this.className = null;
  }

  public ValueMirror(String varName, int refID) {
    this.type = JsDataType.TYPE_OBJECT;
    this.name = varName;
    this.ref = refID;
    this.className = OBJECT_CLASSNAME;
  }

  public ValueMirror(String varName, int refID, PropertyReference[] props, String className) {
    this.type = getObjectJsType(className);
    this.className = className;
    this.name = varName;
    this.ref = refID;
    this.properties = props;
  }

  public String getName() {
    return name;
  }

  public JsDataType getType() {
    return type;
  }

  public PropertyReference[] getProperties() {
    return properties;
  }

  public void setProperties(String className, PropertyReference[] props) {
    if (className != null) {
      this.className = className;
    }
    this.properties = props;
  }

  public int getRef() {
    return ref;
  }

  /**
   * @return the type representation as a String
   */
  public String getTypeAsString() {
    switch (type) {
      case TYPE_NUMBER:
      case TYPE_OBJECT:
      case TYPE_ARRAY:
      case TYPE_FUNCTION:
      case TYPE_DATE:
        return JsDataTypeUtil.getJsonString(type);
      case TYPE_STRING:
      default:
        return "text";
    }
  }

  public void setValue(String val) {
    switch (type) {
      case TYPE_UNDEFINED:
      case TYPE_NULL:
      case TYPE_BOOLEAN:
      case TYPE_DATE:
      case TYPE_STRING:
      case TYPE_NUMBER:
        value = val;
        break;
      case TYPE_OBJECT:
      case TYPE_ARRAY:
      case TYPE_FUNCTION:
      default:
        throw new IllegalStateException("cannot set value of an object");
    }
  }

  public void setType(JsDataType type) {
    this.type = type;
  }

  @Override
  public String toString() {
    switch (type) {
      case TYPE_UNDEFINED:
      case TYPE_NULL:
      case TYPE_DATE:
      case TYPE_STRING:
      case TYPE_NUMBER:
      case TYPE_BOOLEAN:
      case TYPE_REGEXP:
        return value == null
            ? ""
            : value;
      case TYPE_OBJECT:
      case TYPE_ARRAY:
        return "[" + className + "]";
      case TYPE_FUNCTION:
        return "[Function]";
      default:
        return "";
    }
  }

  public String getClassName() {
    return className;
  }

  private static JsDataType getObjectJsType(String className) {
    return JsDataTypeUtil.fromJsonTypeAndClassName("object", className);
  }
}
