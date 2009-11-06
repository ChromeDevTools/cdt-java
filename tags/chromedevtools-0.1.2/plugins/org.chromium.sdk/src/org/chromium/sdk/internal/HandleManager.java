// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.json.simple.JSONObject;

/**
 * A facility for storage and retrieval of handle objects using their "ref" IDs.
 */
public class HandleManager {

  private final ConcurrentMap<Long, JSONObject> refToHandle =
      new ConcurrentHashMap<Long, JSONObject>();

  public void putAll(Map<Long, JSONObject> map) {
    for (Map.Entry<Long, JSONObject> en : map.entrySet()) {
      put(en.getKey(), en.getValue());
    }
  }

  void put(Long ref, JSONObject object) {
    JSONObject oldObject = refToHandle.putIfAbsent(ref, object);
    if (oldObject != null) {
      mergeValues(oldObject, object);
    }
  }

  void put(JSONObject object) {
    Long ref = JsonUtil.getAsLong(object, V8Protocol.REF_HANDLE);
    if (ref != null && ref >= 0) {
      put(ref, object);
    }
  }

  public JSONObject getHandle(Long ref) {
    return refToHandle.get(ref);
  }

  private static void mergeValues(JSONObject oldObject, JSONObject newObject) {
  }
}
