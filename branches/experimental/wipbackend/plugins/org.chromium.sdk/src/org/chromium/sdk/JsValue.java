// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * An object that represents a browser JavaScript VM variable value (compound or
 * atomic.)
 */
public interface JsValue {

  /**
   * Type of a JavaScript value. Two bogus type values (DATE and ARRAY) are
   * included even though they are not reported by V8. Instead, they are inferred
   * from the object classname. {@code null} value has a bogus type NULL.
   */
  public enum Type {

    /**
     * Object type.
     */
    TYPE_OBJECT,

    /**
     * Number type.
     */
    TYPE_NUMBER,

    /**
     * String type.
     */
    TYPE_STRING,

    /**
     * Function type.
     */
    TYPE_FUNCTION,

    /**
     * Boolean type.
     */
    TYPE_BOOLEAN,

    /**
     * Error type (this one describes a JavaScript exception).
     */
    TYPE_ERROR,

    /**
     * A regular expression.
     */
    TYPE_REGEXP,

    /**
     * An object which is actually a Date. This type is not present in the
     * protocol but is rather induced from the "object" type and "Date" class of
     * an object.
     */
    TYPE_DATE,

    /**
     * An object which is actually an array. This type is not present in the
     * protocol but is rather induced from the "object" type and "Array" class of
     * an object.
     */
    TYPE_ARRAY,

    /**
     * undefined type.
     */
    TYPE_UNDEFINED,

    /**
     * null type. This is a bogus type that doesn't exist in JavaScript.
     */
    TYPE_NULL;

    /**
     * Returns whether {@code type} corresponds to a JsObject. Note that while 'null' is an object
     * in JavaScript world, here for API consistency it has bogus type {@link #TYPE_NULL} and is
     * not a {@link JsObject}.
     * @param type to check
     */
    public static boolean isObjectType(Type type) {
      return type == TYPE_OBJECT || type == TYPE_ARRAY || type == TYPE_ERROR ||
          type == TYPE_FUNCTION;
    }
  }

  /**
   * @return this value type
   */
  Type getType();

  /**
   * @return a string representation of this value
   */
  String getValueString();

  /**
   * Return this value cast to {@link JsObject} or {@code null} if this value
   * is not an object.
   * See {@link Type#isObjectType(Type)} method for details.
   *
   * @return this or null
   */
  JsObject asObject();

  /**
   * @return whether the value of the object has been truncated while loaded
   */
  boolean isTruncated();

  interface ReloadBiggerCallback {
    void done();
  }

  /**
   * Asynchronously reloads object value with extended size limit.
   */
  RelayOk reloadHeavyValue(ReloadBiggerCallback callback, SyncCallback syncCallback);
}
