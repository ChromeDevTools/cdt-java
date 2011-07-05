// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.getSafe;

import java.util.HashMap;
import java.util.Map;

/**
 * Dual-key map that works with {@link VmResourceId}. Both script name and script id are
 * used as keys. No conflicts are really expected -- only guard runtime exceptions are
 * provided.
 */
public class VmResourceIdMap<T> {
  private final Map<String, T> scriptNameMap = new HashMap<String, T>();
  private final Map<Long, T> scriptIdMap = new HashMap<Long, T>();

  public T get(VmResourceId resourceId) {
    Long scriptId = resourceId.getId();
    if (scriptId != null) {
      T result = getSafe(scriptIdMap, scriptId);
      if (result != null) {
        return result;
      }
    }
    return getByName(resourceId.getName());
  }

  public T getByName(String scriptName) {
    if (scriptName != null) {
      T result = getSafe(scriptNameMap, scriptName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  public void put(VmResourceId resourceId, T data) {
    Long scriptId = resourceId.getId();
    if (scriptId != null) {
      Object conflict = scriptIdMap.put(scriptId, data);
      if (conflict != null) {
        throw new RuntimeException();
      }
    }
    String scriptName = resourceId.getName();
    if (scriptName != null) {
      Object conflict = scriptNameMap.put(scriptName, data);
      if (conflict != null) {
        throw new RuntimeException();
      }
    }
  }

  public void remove(VmResourceId resourceId) {
    Long scriptId = resourceId.getId();
    if (scriptId != null) {
      scriptIdMap.remove(scriptId);
    }
    String scriptName = resourceId.getName();
    if (scriptName != null) {
      scriptNameMap.remove(scriptName);
    }
  }

  public void clear() {
    scriptIdMap.clear();
    scriptNameMap.clear();
  }
}
