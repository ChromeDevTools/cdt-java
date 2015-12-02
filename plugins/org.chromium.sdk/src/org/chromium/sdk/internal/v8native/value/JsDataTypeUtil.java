// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.value;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.JsValue.Type;

/**
 * A utility that facilitates retrieval of {@link Type}s according to the
 * JSON values received.
 */
public class JsDataTypeUtil {

  private static Map<String, Type> jsonTypeToEnum = new HashMap<String, Type>();

  private static Map<Type, String> enumToJsonType = new EnumMap<Type, String>(Type.class);

  /**
   * Class name of an Array object (TYPE_ARRAY).
   */
  public static final String CLASSNAME_ARRAY = "Array";

  public static final String JSON_NULL_TYPE = "null";
  public static final String JSON_UNDEFINED_TYPE = "undefined";
  public static final String JSON_BOOLEAN_TYPE = "boolean";
  public static final String JSON_NUMBER_TYPE = "number";
  public static final String JSON_STRING_TYPE = "string";

  /**
   * Class name of a Date object (TYPE_DATE).
   */
  private static final String CLASSNAME_DATE = "Date";

  static {
    put("object", Type.TYPE_OBJECT);
    put(JSON_NUMBER_TYPE, Type.TYPE_NUMBER);
    put(JSON_STRING_TYPE, Type.TYPE_STRING);
    put("function", Type.TYPE_FUNCTION);
    put(JSON_BOOLEAN_TYPE, Type.TYPE_BOOLEAN);
    put(JSON_UNDEFINED_TYPE, Type.TYPE_UNDEFINED);
    put(JSON_NULL_TYPE, Type.TYPE_NULL);
    put("error", Type.TYPE_ERROR);
    put("array", Type.TYPE_ARRAY);
    put("date", Type.TYPE_DATE);
    put("regexp", Type.TYPE_REGEXP);
  }

  /**
   * Gets a JsDataType using a V8 JavaScript type and, optionally, a class name
   * of the object. If {@code className} is {@code null}, only the 1:1 mapping
   * shall be used.
   *
   * @param jsonType the JS type from a JSON response
   * @param className a nullable class name of the object
   * @return a JsDataType corresponding to {@code jsonType} and, possibly,
   *         modified according to {@code className}
   */
  public static Type fromJsonTypeAndClassName(String jsonType, String className) {
    if (jsonType == null) {
      return null;
    }
    if (CLASSNAME_DATE.equals(className)) {
      // hack to use the TYPE_DATE type even though its type in V8 is "object"
      return Type.TYPE_DATE;
    } else if (CLASSNAME_ARRAY.equals(className)) {
      // hack to use the TYPE_ARRAY type even though its type in V8 is "object"
      return Type.TYPE_ARRAY;
    }
    return jsonTypeToEnum.get(jsonType);
  }

  /**
   * Converts {@code type} to its JSON representation used in V8.
   *
   * @param type to convert
   * @return a string name of the type understandable by V8
   */
  public static String getJsonString(Type type) {
    return enumToJsonType.get(type);
  }

  private static void put(String jsonString, Type type) {
    jsonTypeToEnum.put(jsonString, type);
    enumToJsonType.put(type, jsonString);
  }

  private JsDataTypeUtil() {
    // not instantiable
  }
}
