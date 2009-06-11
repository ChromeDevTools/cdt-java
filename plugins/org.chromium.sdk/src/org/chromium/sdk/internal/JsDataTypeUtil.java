// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.JsDataType;

/**
 * A utility that facilitates retrieval of {@link JsDataType}s according to the
 * JSON values received.
 */
public class JsDataTypeUtil {

  private static Map<String, JsDataType> jsonTypeToEnum = new HashMap<String, JsDataType>();

  private static Map<JsDataType, String> enumToJsonType = new HashMap<JsDataType, String>();

  /**
   * Class name of an Array object (TYPE_ARRAY).
   */
  public static final String CLASSNAME_ARRAY = "Array";

  /**
   * Class name of a Date object (TYPE_DATE).
   */
  private static final String CLASSNAME_DATE = "Date";

  static {
    put("object", JsDataType.TYPE_OBJECT);
    put("number", JsDataType.TYPE_NUMBER);
    put("string", JsDataType.TYPE_STRING);
    put("function", JsDataType.TYPE_FUNCTION);
    put("boolean", JsDataType.TYPE_BOOLEAN);
    put("undefined", JsDataType.TYPE_UNDEFINED);
    put("null", JsDataType.TYPE_NULL);
    put("array", JsDataType.TYPE_ARRAY);
    put("date", JsDataType.TYPE_DATE);
  }

  /**
   * Gets a JsDataType using a V8 Javascript type and, optionally, a class name
   * of the object. If {@code className} is {@code null}, only the 1:1 mapping
   * shall be used.
   *
   * @param jsonType the JS type from a JSON response
   * @param className a nullable class name of the object
   * @return a JsDataType corresponding to {@code jsonType} and, possibly,
   *         modified according to {@code className}
   */
  public static JsDataType fromJsonTypeAndClassName(String jsonType, String className) {
    if (jsonType == null) {
      return null;
    }
    if (CLASSNAME_DATE.equals(className)) {
      // hack to use the TYPE_DATE type even though its type in V8 is "object"
      return JsDataType.TYPE_DATE;
    } else if (CLASSNAME_ARRAY.equals(className)) {
      // hack to use the TYPE_ARRAY type even though its type in V8 is "object"
      return JsDataType.TYPE_ARRAY;
    }
    return jsonTypeToEnum.get(jsonType);
  }

  /**
   * Converts {@code type} to its JSON representation used in V8.
   *
   * @param type to convert
   * @return a string name of the type understandable by V8
   */
  public static String getJsonString(JsDataType type) {
    return enumToJsonType.get(type);
  }

  private static void put(String jsonString, JsDataType type) {
    jsonTypeToEnum.put(jsonString, type);
    enumToJsonType.put(type, jsonString);
  }

  private JsDataTypeUtil() {
    // not instantiable
  }
}
