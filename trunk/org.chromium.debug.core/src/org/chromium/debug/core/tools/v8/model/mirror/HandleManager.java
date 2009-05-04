// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.model.mirror;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

/**
 * Stores and retrieves handles using their "ref" IDs.
 */
public class HandleManager {

  private final Map<Long, JSONObject> refToHandle =
      new HashMap<Long, JSONObject>();

  void reset() {
    refToHandle.clear();
  }

  void putAll(Map<Long, JSONObject> map) {
    refToHandle.putAll(map);
  }

  public JSONObject getHandle(Long ref) {
    return refToHandle.get(ref);
  }
}
