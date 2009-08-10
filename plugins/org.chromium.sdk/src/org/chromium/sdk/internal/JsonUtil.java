// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A utility for JSON-related data conversion.
 */
public class JsonUtil {

  private static final Logger LOGGER = Logger.getLogger(JsonUtil.class.getName());

  /**
   * Converts a JSONStreamAware into a String.
   *
   * @param object the object to convert
   * @return a JSON String representation of the object
   */
  public static String streamAwareToJson(JSONStreamAware object) {
    StringWriter out = new StringWriter();
    try {
      object.writeJSONString(out);
    } catch (IOException e) {
      return null;
    }
    return out.toString();
  }

  /**
   * @param json a JSON representation of an object (rather than an array or any
   *        other type)
   * @return a JSONObject represented by json, or null if json does not
   *         represent a valid JSONObject
   * @throws ParseException
   */
  public static JSONObject jsonObjectFromJson(String json) throws ParseException {
    JSONParser p = new JSONParser();
    Object parsed = p.parse(json);
    if (false == parsed instanceof JSONObject) {
      LOGGER.log(Level.SEVERE, "Not a JSON object: {0}", json);
      return null;
    }
    return (JSONObject) parsed;
  }

  /**
   * Helper function to rip out an integer number from a JSON payload
   *
   * @param obj JSON payload
   * @param key to look up
   * @return null if key not found or bad type
   */
  public static Long getAsLong(JSONObject obj, CharSequence key) {
    String keyString = key.toString();
    Object v = obj.get(keyString);
    if (v instanceof Long || v == null) {
      return (Long) v;
    }

    LOGGER.log(Level.SEVERE, "Key: {0}, found value: {1}", new Object[] {keyString, v});
    return null;
  }

  /**
   * Helper function to rip out a double from a JSON payload
   *
   * @param obj JSON payload
   * @param key to look up
   * @return null if key not found or bad type
   */
  public static Double getAsDouble(JSONObject obj, CharSequence key) {
    String keyString = key.toString();
    Object v = obj.get(keyString);
    if (v instanceof Double || v == null) {
      return (Double) v;
    }
    LOGGER.log(Level.SEVERE, "Key: {0}, found value: {1}", new Object[] {keyString, v});
    return null;
  }

  /**
   * Helper function to rip out a string from a JSON payload
   *
   * @param obj JSON payload
   * @param key to look up
   * @return null if key not found or bad type
   */
  public static String getAsString(JSONObject obj, CharSequence key) {
    String keyString = key.toString();
    Object v = obj.get(keyString);
    if (v instanceof String || v == null) {
      return (String) v;
    }
    return String.valueOf(v);
  }

  /**
   * Helper function to rip out a Boolean from a JSON payload
   *
   * @param obj JSON payload
   * @param key to look up
   * @return Boolean.FALSE if key not found
   */
  public static Boolean getAsBoolean(JSONObject obj, CharSequence key) {
    String keyString = key.toString();
    Object v = obj.get(keyString);
    if (v instanceof Boolean || v == null) {
      return v != null
          ? (Boolean) v
          : false;
    }

    LOGGER.log(Level.SEVERE, "Key: {0}, found value: {1}", new Object[] {keyString, v});
    return false;
  }

  /**
   * Helper function to rip out a nested JSON object from the payload
   *
   * @param obj JSON payload
   * @param key to look up
   * @return null if key not found
   */
  public static JSONObject getAsJSON(JSONObject obj, CharSequence key) {
    String keyString = key.toString();
    Object v = obj.get(keyString);
    if (v instanceof JSONObject || v == null) {
      return (JSONObject) v;
    }

    LOGGER.log(Level.SEVERE, "Key: {0}, found value: {1}", new Object[] {keyString, v});
    return null;
  }

  /**
   * Helper function to rip out a JSONArray from the payload
   *
   * @param obj JSON payload
   * @param key to look up
   * @return null if key not found
   */
  public static JSONArray getAsJSONArray(JSONObject obj, CharSequence key) {
    String keyString = key.toString();
    Object v = obj.get(keyString);
    if (v instanceof JSONArray || v == null) {
      return (JSONArray) v;
    }

    LOGGER.log(Level.SEVERE, "Key: {0}, found value: {1}", new Object[] {keyString, v});
    return null;
  }

  /**
   * @param value to check
   * @return whether the value can be parsed as an integer
   */
  public static boolean isInteger(String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * @param response to get the body from
   * @return the "body" value
   */
  public static JSONObject getBody(JSONObject response) {
    return JsonUtil.getAsJSON(response, V8Protocol.KEY_BODY);
  }

  /**
   * @param response to get the status from
   * @return the "success" value
   */
  public static boolean isSuccessful(JSONObject response) {
    return JsonUtil.getAsBoolean(response, V8Protocol.KEY_SUCCESS);
  }

  private JsonUtil() {
    // not instantiable
  }
}
