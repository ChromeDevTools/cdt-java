// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.JsValue.Type;
import org.json.simple.JSONObject;

/**
 * A representation of a datum (value) in the remote JavaScript VM.
 */
public class ValueMirror {

  /**
   * Class name of a generic object.
   */
  private static final String OBJECT_CLASSNAME = "Object";

  /**
   * A named property reference.
   */
  public static class PropertyReference {
    private final int ref;

    private final String name;

    private final JSONObject valueObject;

    private PropertyReference(int refId, String propertyName, JSONObject valueObject) {
      this.ref = refId;
      this.name = propertyName;
      this.valueObject = valueObject;
    }

    public int getRef() {
      return ref;
    }

    public String getName() {
      return name;
    }

    public JSONObject getValueObject() {
      return valueObject;
    }
  }

  private final int ref;

  private Type type;

  private String value;

  private PropertyReference[] properties = null;

  private String className;

  /**
   * Constructs a new PropertyReference instance given its "ref" value and name.
   *
   * @param refId the "ref" value of this property
   * @param propertyName the name of the property
   * @param valueObject a JSON descriptor of the property
   * @return a new PropertyReference instance
   */
  public static PropertyReference newPropertyReference(int refId, String propertyName,
      JSONObject valueObject) {
    return new PropertyReference(refId, propertyName, valueObject);
  }

  public ValueMirror(String value, Type type, String className) {
    this.type = type;
    this.value = value;
    this.ref = -1;
    this.className = className;
  }

  public ValueMirror(int refID) {
    this.type = Type.TYPE_OBJECT;
    this.ref = refID;
    this.className = OBJECT_CLASSNAME;
  }

  public ValueMirror(int refID, PropertyReference[] props, String className) {
    this.type = getObjectJsType(className);
    this.className = className;
    this.ref = refID;
    this.properties = props;
  }

  public Type getType() {
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

  public void setType(Type type) {
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

  private static Type getObjectJsType(String className) {
    return JsDataTypeUtil.fromJsonTypeAndClassName("object", className);
  }
}
