// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import java.util.HashMap;
import java.util.Map;

/**
 * Known V8 debugger protocol message types.
 */
public enum V8MessageType {

  REQUEST("request"),
  RESPONSE("response"),
  EVENT("event"),
  ;

  private static final Map<String, V8MessageType> map = new HashMap<String, V8MessageType>();

  static {
    for (V8MessageType type : values()) {
      map.put(type.value, type);
    }
  }

  public final String value;

  private V8MessageType(String value) {
    this.value = value;
  }

  public static V8MessageType forString(String value) {
    if (value == null) {
      return null;
    }
    return map.get(value);
  }
}
