// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.Script;
import org.chromium.sdk.Script.Type;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.PropertyType;
import org.chromium.sdk.internal.ValueMirror;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.request.ScriptsMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A utility class to process V8 debugger messages.
 */
public class V8ProtocolUtil {

  /**
   * Computes a script type given a V8 Long type value
   *
   * @param typeNumber a type designator from a V8 JSON response
   * @return a type corresponding to {@code typeNumber} or {@code null} if
   *         {@code typeNumber == null}
   */
  public static Script.Type getScriptType(Long typeNumber) {
    if (typeNumber == null) {
      return null;
    }
    switch (typeNumber.intValue()) {
      case ScriptsMessage.SCRIPTS_NORMAL:
        return Type.NORMAL;
      case ScriptsMessage.SCRIPTS_NATIVE:
        return Type.NATIVE;
      case ScriptsMessage.SCRIPTS_EXTENSION:
        return Type.EXTENSION;
      default:
        throw new IllegalArgumentException("unknown script type: " + typeNumber);
    }
  }

  /**
   * Returns the value of "ref" field in object corresponding to the fieldName
   * in parent.
   *
   * @param parent to get the object from
   * @param fieldName of the object to get the "ref" from
   * @return ref value or null if fieldName or "ref" not found
   */
  public static Long getObjectRef(JSONObject parent, CharSequence fieldName) {
    JSONObject child = JsonUtil.getAsJSON(parent, fieldName.toString());
    if (child == null) {
      return null;
    }
    return JsonUtil.getAsLong(child, V8Protocol.REF);
  }

  /**
   * Maps handle "ref" values to the handles themselves for a quick lookup.
   *
   * @param handles JSONArray of handles received as the "refs" field value
   * @return a mapping of handle ref values to handles
   */
  public static Map<Long, JSONObject> getRefHandleMap(List<?> handles) {
    Map<Long, JSONObject> result = new HashMap<Long, JSONObject>();
    for (int i = 0, size = handles.size(); i < size; ++i) {
      JSONObject handle = (JSONObject) handles.get(i);
      putHandle(result, handle);
    }
    return result;
  }

  /**
   * Puts a single {@code handle} into the {@code targetMap}, using the "handle"
   * field as the map key.
   *
   * @param targetMap to put the handle into.
   * @param handle to put
   */
  public static void putHandle(Map<Long, JSONObject> targetMap, JSONObject handle) {
    Long refValue = JsonUtil.getAsLong(handle, V8Protocol.REF_HANDLE);
    targetMap.put(refValue, handle);
  }

  /**
   * Gets a reference number associated with the given ref object.
   *
   * @param refObject the ref object
   * @return reference number or -1 if no reference value
   */
  public static Long getValueRef(JSONObject refObject) {
    JSONObject argValue = JsonUtil.getAsJSON(refObject, V8Protocol.ARGUMENT_VALUE);
    if (argValue != null) {
      Long argValueRef = JsonUtil.getAsLong(argValue, V8Protocol.REF);
      if (argValueRef != null) {
        return argValueRef;
      }
    }
    return -1L;
  }

  /**
   * Constructs {@code PropertyReference}s from the specified object.
   *
   * @param handle containing the object specification from the V8 debugger
   * @return {@code PropertyReference}s based on the {@code handle} data
   */
  public static PropertyReference[] extractObjectProperties(JSONObject handle) {
    JSONArray props = JsonUtil.getAsJSONArray(handle, V8Protocol.REF_PROPERTIES);
    int propsLen = props.size();
    List<PropertyReference> objProps = new ArrayList<PropertyReference>(propsLen);

    for (int i = 0; i < propsLen; i++) {
      JSONObject prop = (JSONObject) props.get(i);
      int ref = JsonUtil.getAsLong(prop, V8Protocol.REF).intValue();
      String name = JsonUtil.getAsString(prop, V8Protocol.REF_PROP_NAME);
      if (name == null) {
        name = String.valueOf(JsonUtil.getAsLong(prop, V8Protocol.REF_PROP_NAME));
      }
      Long propType = JsonUtil.getAsLong(prop, V8Protocol.REF_PROP_TYPE);

      if (isInternalProperty(name)) {
        continue;
      }

      // propType is NORMAL by default
      int propTypeValue = propType != null
          ? propType.intValue()
          : PropertyType.NORMAL.value;
      if (propTypeValue == PropertyType.FIELD.value ||
          propTypeValue == PropertyType.CALLBACKS.value ||
          propTypeValue == PropertyType.NORMAL.value) {
        objProps.add(ValueMirror.newPropertyReference(ref, name));
      }
    }

    return objProps.toArray(new PropertyReference[objProps.size()]);
  }

  /**
   * @param propertyName the property name to check
   * @return whether the given property name corresponds to an internal V8
   *         property
   */
  public static boolean isInternalProperty(String propertyName) {
    // Chrome can return properties like ".arguments". They should be ignored.
    return propertyName.startsWith(".");
  }

  /**
   * Gets a function name from the given function handle.
   *
   * @param functionObject the function handle
   * @return the actual of inferred function name. Will handle {@code null} or
   *         unnamed functions
   */
  public static String getFunctionName(JSONObject functionObject) {
    if (functionObject == null) {
      return "<unknown>";
    } else {
      String name = getNameOrInferred(functionObject, V8Protocol.LOCAL_NAME);
      if (isNullOrEmpty(name)) {
        return "(anonymous function)";
      } else {
        return name;
      }
    }
  }

  /**
   * Gets a script id from a script response.
   *
   * @param scriptObject to get the "id" value from
   * @return the script id
   */
  public static Long getScriptIdFromResponse(JSONObject scriptObject) {
    return JsonUtil.getAsLong(scriptObject, V8Protocol.ID);
  }

  private static String getNameOrInferred(JSONObject obj, V8Protocol nameProperty) {
    String name = JsonUtil.getAsString(obj, nameProperty);
    if (isNullOrEmpty(name)) {
      name = JsonUtil.getAsString(obj, V8Protocol.INFERRED_NAME);
    }
    return name;
  }

  private static boolean isNullOrEmpty(String value) {
    return value == null || value.length() == 0;
  }

}
