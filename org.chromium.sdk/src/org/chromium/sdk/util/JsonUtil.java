// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.runtime.Status;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A utility for JSON-related data conversion.
 */
public class JsonUtil {

  /**
   * Converts a JSONStreamAware into a String.
   * 
   * @param object
   *          the object to convert
   * @return a JSON String representation of the object
   * @throws IOException
   */
  public static String streamAwareToJson(JSONStreamAware object)
      throws IOException {
    StringWriter out = new StringWriter();
    object.writeJSONString(out);
    return out.toString();
  }

  /**
   * @param json
   *          a JSON representation of an object (rather than an array or any
   *          other type)
   * @return a JSONObject represented by json, or null if json does not
   *         represent a valid JSONObject
   */
  public static JSONObject jsonObjectFromJson(String json) {
    JSONParser p = new JSONParser();
    try {
      Object parsed = p.parse(json);
      if (false == parsed instanceof JSONObject) {
        ChromiumDebugPlugin.log(new Status(Status.ERROR,
            ChromiumDebugPlugin.PLUGIN_ID, MessageFormat.format(
                "Not a json object: {0}", json))); //$NON-NLS-1$
        return null;
      }
      return (JSONObject) parsed;
    } catch (ParseException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
  }

  /**
   * Helper function to rip out an integer number from a JSON payload
   * 
   * @param obj
   *          JSON payload
   * @param key
   *          to look up
   * @return null if key not found or bad type
   */
  static public Long getAsLong(JSONObject obj, String key) {
    Object v = obj.get(key);
    if (v instanceof Long || v == null) {
      return (Long) v;
    }

    ChromiumDebugPlugin.logError("Key: {0}, found value: {1}", key, v); //$NON-NLS-1$
    return null;
  }

  /**
   * Helper function to rip out an FP number from a JSON payload
   * 
   * @param obj
   *          JSON payload
   * @param key
   *          to look up
   * @return null if key not found or bad type
   */
  static public Double getAsDouble(JSONObject obj, String key) {
    Object v = obj.get(key);
    if (v instanceof Double || v == null) {
      return (Double) v;
    }
    ChromiumDebugPlugin.logError("Key: {0}, found value: {1}", key, v); //$NON-NLS-1$
    return null;
  }

  /**
   * Helper function to rip out a string from a JSON payload
   * 
   * @param obj
   *          JSON payload
   * @param key
   *          to look up
   * @return null if key not found or bad type
   */
  static public String getAsString(JSONObject obj, String key) {
    Object v = obj.get(key);
    if (v instanceof String || v == null) {
      return (String) v;
    }

    ChromiumDebugPlugin.logError("Key: {0}, found value: {1}", key, v); //$NON-NLS-1$
    return null;
  }

  /**
   * Helper function to rip out a Boolean from a JSON payload
   * 
   * @param obj
   *          JSON payload
   * @param key
   *          to look up
   * @return Boolean.FALSE if key not found
   */
  static public Boolean getAsBoolean(JSONObject obj, String key) {
    Object v = obj.get(key);
    if (v instanceof Boolean || v == null) {
      return (Boolean) v;
    }

    ChromiumDebugPlugin.logError("Key: {0}, found value: {1}", key, v); //$NON-NLS-1$
    return Boolean.FALSE;
  }

  /**
   * Helper function to rip out a nested JSON object from the payload
   * 
   * @param obj
   *          JSON payload
   * @param key
   *          to look up
   * @return null if key not found
   */
  static public JSONObject getAsJSON(JSONObject obj, String key) {
    Object v = obj.get(key);
    if (v instanceof JSONObject || v == null) {
      return (JSONObject) v;
    }

    ChromiumDebugPlugin.logError("Key: {0}, found value: {1}", key, v); //$NON-NLS-1$
    return null;
  }

  /**
   * Helper function to rip out a JSONArray from the payload
   * 
   * @param obj
   *          JSON payload
   * @param key
   *          to look up
   * @return null if key not found
   */
  static public JSONArray getAsJSONArray(JSONObject obj, String key) {
    Object v = obj.get(key);
    if (v instanceof JSONArray || v == null) {
      return (JSONArray) v;
    }

    ChromiumDebugPlugin.logError("Key: {0}, found value: {1}", key, v); //$NON-NLS-1$
    return null;
  }

  /**
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

  private JsonUtil() {
    // not instantiable
  }
}
