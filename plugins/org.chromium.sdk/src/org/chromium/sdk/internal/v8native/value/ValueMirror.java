// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.JsValue.Type;

/**
 * A representation of a datum (value) in the remote JavaScript VM. Must contain all the
 * data immutable, except for properties. Reference to properties is optional and may be set later.
 */
public class ValueMirror {

  public static PropertyHoldingValueMirror createScalar(LoadableString stringValue,
      Type type, String className) {
    return new ValueMirror(stringValue, type, className).getProperties();
  }

  public static PropertyHoldingValueMirror createObject(int refID,
      SubpropertiesMirror subpropertiesMirror, Type type, String className) {
    if (subpropertiesMirror == null) {
      throw new NullPointerException();
    }
    return new ValueMirror(refID, subpropertiesMirror, type, className).getProperties();
  }

  public static ValueMirror createObjectUnknownProperties(int refID, Type type, String className) {
    return new ValueMirror(refID, null, type, className);
  }

  private final int ref;

  private final Type type;

  private final LoadableString stringValue;

  private final String className;

  private volatile PropertyHoldingValueMirror properties = null;

  private ValueMirror(LoadableString stringValue, Type type, String className) {
    this.type = type;
    this.stringValue = stringValue;
    this.ref = -1;
    this.className = className;
    this.properties = new PropertyHoldingValueMirror(this);
  }

  private ValueMirror(int refID, SubpropertiesMirror subpropertiesMirror, Type type,
      String className) {
    this.type = type;
    this.className = className;
    this.ref = refID;
    PropertyHoldingValueMirror propertiesMirror;
    if (subpropertiesMirror == null) {
      propertiesMirror = null;
    } else {
      propertiesMirror = new PropertyHoldingValueMirror(this, subpropertiesMirror);
    }
    this.properties = propertiesMirror;
    this.stringValue = null;
  }

  public Type getType() {
    return type;
  }

  public PropertyHoldingValueMirror getProperties() {
    return properties;
  }

  public int getRef() {
    return ref;
  }

  public LoadableString getStringValue() {
    return stringValue;
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
        return stringValue == null
            ? ""
            : stringValue.getCurrentString();
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

  void mergeFrom(ValueMirror alternative) {
    synchronized (MERGE_VALUE_MIRROR_MONITOR) {
      if (alternative.properties != null) {
        if (this.properties == null) {
          this.properties =
              new PropertyHoldingValueMirror(this, alternative.properties.getSubpropertiesMirror());
        } else {
          mergeProperties(this.properties, alternative.properties);
        }
      }
    }
  }

  private static final Object MERGE_VALUE_MIRROR_MONITOR = new Object();

  /**
   * Merge a record from data base with a new record just received. Theoretically
   * the new record may have something that base version lacks.
   * However,this method is more of symbolic use right now.
   *
   * @param baseProperties record version which is kept in database
   * @param altProperties record version that just has come from outside and may
   *        contain additional data
   */
  private static void mergeProperties(PropertyHoldingValueMirror baseProperties,
      PropertyHoldingValueMirror altProperties) {
  }
}
