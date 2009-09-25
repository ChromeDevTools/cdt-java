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
import org.chromium.sdk.internal.PropertyReference;
import org.chromium.sdk.internal.PropertyType;
import org.chromium.sdk.internal.ProtocolOptions;
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
   * Constructs {@code PropertyReference}s from the specified object, be it in
   * the "original" or "inlineRefs" format.
   *
   * @param handle to get property references from
   * @return an array of PropertyReferences
   */
  public static List<? extends PropertyReference> extractObjectProperties(JSONObject handle) {
    JSONArray props = JsonUtil.getAsJSONArray(handle, V8Protocol.REF_PROPERTIES);
    int propsLen = props.size();
    List<PropertyReference> objProps = new ArrayList<PropertyReference>(propsLen);
    for (int i = 0; i < propsLen; i++) {
      JSONObject prop = (JSONObject) props.get(i);
      putMirror(objProps, prop, V8Protocol.REF_VALUE, PropertyNameGetter.SUBPROPERTY);
    }

    return objProps;
  }

  static void putMirror(List<PropertyReference> refs, JSONObject propertyObject,
      CharSequence valuePropertyName, V8ProtocolUtil.PropertyNameGetter nameGetter) {
    PropertyReference propertyRef = V8ProtocolUtil.extractProperty(propertyObject,
        valuePropertyName, nameGetter);
    if (propertyRef != null) {
      refs.add(propertyRef);
    }
  }

  /**
   * Constructs one {@code PropertyReference} from the specified object, be it in
   * the "original" or "inlineRefs" format.
   *
   * @param prop json object
   * @param valuePropertyName name of value property in this prop object, might be null
   * @return PropertyReference or null if we ignore this property
   */
  public static PropertyReference extractProperty(JSONObject prop, CharSequence valuePropertyName,
      PropertyNameGetter nameGetter) {
    String name = nameGetter.getName(prop, null);
    if (name == null) {
      return null;
    }
    JSONObject propValue = null;
    if (valuePropertyName != null) {
      propValue = JsonUtil.getAsJSON(prop, valuePropertyName);
    }
    if (propValue == null) {
      // Handle the original (non-"inlineRefs") format that contains the
      // value data in the outer objects.
      propValue = prop;
    }
    if (isInternalProperty(name)) {
      return null;
    }

    Long propType = JsonUtil.getAsLong(propValue, V8Protocol.REF_PROP_TYPE);
    // propType is NORMAL by default
    int propTypeValue = propType != null
        ? propType.intValue()
        : PropertyType.NORMAL.value;
    if (propTypeValue == PropertyType.FIELD.value ||
        propTypeValue == PropertyType.CALLBACKS.value ||
        propTypeValue == PropertyType.NORMAL.value) {
      Long longRef = JsonUtil.getAsLong(propValue, V8Protocol.REF);
      return new PropertyReference(longRef.intValue(), name, propValue);
    }
    return null;
  }

  enum PropertyNameGetter {
    ARGUMENT() {
      @Override
      String getName(JSONObject ref, JSONObject value) {
        // an unnamed actual argument (there is no formal counterpart in the
        // method signature) that will be available in the "arguments" object
        return JsonUtil.getAsString(ref, V8Protocol.ARGUMENT_NAME);
      }
    },
    LOCAL() {
      @Override
      String getName(JSONObject ref, JSONObject value) {
        String name = JsonUtil.getAsString(ref, V8Protocol.LOCAL_NAME);
        if (V8ProtocolUtil.isInternalProperty(name)) {
          return null;
        }
        return name;
      }
    },
    THIS() {
      @Override
      String getName(JSONObject ref, JSONObject value) {
        /** The name of the "this" object to report as a variable name. */
        return  "this";
      }
    },
    SUBPROPERTY() {
      @Override
      String getName(JSONObject ref, JSONObject value) {
        return getPropertyName(ref);
      }
    };

    /**
     * @return property name or null if we should skip this property
     */
    abstract String getName(JSONObject ref, JSONObject value);
  }

  private static String getPropertyName(JSONObject prop) {
    String name = JsonUtil.getAsString(prop, V8Protocol.REF_PROP_NAME);
    if (name == null) {
      name = String.valueOf(JsonUtil.getAsLong(prop, V8Protocol.REF_PROP_NAME));
    }
    return name;
  }

  /**
   * @param propertyName the property name to check
   * @return whether the given property name corresponds to an internal V8
   *         property
   */
  public static boolean isInternalProperty(String propertyName) {
    // Chrome can return properties like ".arguments". They should be ignored.
    return propertyName.length() == 0 || propertyName.startsWith(".");
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

  /**
   * Determines if a {@code script} is valid in the current debug context.
   * Returns {@code null} if it is not, otherwise returns {@code script}.
   *
   * @param script to check and, possibly, modify
   * @param refs from the corresponding V8 response
   * @return script with a non-null name if the script is valid, {@code null}
   *         otherwise
   */
  public static JSONObject validScript(JSONObject script, JSONArray refs,
      ProtocolOptions protocolOptions) {
    Long contextRef = V8ProtocolUtil.getObjectRef(script, V8Protocol.CONTEXT);
    for (int i = 0, size = refs.size(); i < size; i++) {
      JSONObject ref = (JSONObject) refs.get(i);
      if (JsonUtil.getAsLong(ref, V8Protocol.REF_HANDLE).longValue() != contextRef.longValue()) {
        continue;
      }
      JSONObject data = JsonUtil.getAsJSON(ref, V8Protocol.DATA);
      if (data == null && protocolOptions.requireDataField()) {
        return null;
      }
      return script;
    }
    return null; // good context not found
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
