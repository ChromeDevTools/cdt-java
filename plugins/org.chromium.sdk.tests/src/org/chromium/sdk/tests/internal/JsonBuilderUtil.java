// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.internal;

import java.util.Arrays;

import org.chromium.sdk.internal.JsonUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Helper class for creating JSON object in functional style. Introduces
 * "pseudo-json" notation, which is normal JSON with single quotes instead of
 * double quotes -- such notation is easier to be used from Java.
 */
public class JsonBuilderUtil {
  public static JsonPropertyTemp jsonProperty(String name, String value) {
    return new JsonPropertyTemp(name, value);
  }

  public static JsonPropertyTemp jsonProperty(String name, Number value) {
    return new JsonPropertyTemp(name, value);
  }

  public static JsonPropertyTemp jsonProperty(String name, JSONObject value) {
    return new JsonPropertyTemp(name, value);
  }

  public static JsonPropertyTemp jsonProperty(String name, JSONArray value) {
    return new JsonPropertyTemp(name, value);
  }

  public static JSONObject jsonObject(JsonPropertyTemp ... properties) {
    JSONObject object = new JSONObject();
    for (JsonPropertyTemp pr1 : properties) {
      object.put(pr1.getName(), pr1.getValue());
    }
    return object;
  }

  /**
   * Constructs json object from pseudo-json text
   */
  public static JSONObject jsonObject(String pseudoJsonText) {
    String jsonText = "{" + convertToRealJson(pseudoJsonText) + "}";
    try {
      return JsonUtil.jsonObjectFromJson(jsonText);
    } catch (ParseException e) {
      throw new RuntimeException("Failed to parse json", e);
    }
  }

  public static JSONArray jsonArray(JSONObject ... objects) {
    JSONArray array = new JSONArray();
    array.addAll(Arrays.asList(objects));
    return array;
  }

  /**
   * Converts pseudo-json notation to normal json notation.
   * @param pseudoJson json notation with single quotes in place of double quotes
   * @return regular json notation
   */
  public static String convertToRealJson(String pseudoJson) {
    return pseudoJson.replace('\'', '"');
  }

  /**
   * Temporary class that represents JavaScript property. User is not supposed
   * to use it in any way, but simply to pass it back to {@link JsonBuilderUtil}.
   */
  public static class JsonPropertyTemp {
    JsonPropertyTemp(String name, Object value) {
      this.name = name;
      this.value = value;
    }

    String getName() {
      return name;
    }
    Object getValue() {
      return value;
    }
    private final String name;
    private final Object value;
  }

}
