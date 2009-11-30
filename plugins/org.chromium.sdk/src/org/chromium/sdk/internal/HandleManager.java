// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.json.simple.JSONObject;

/**
 * A facility for storage and retrieval of handle objects using their "ref" IDs.
 */
public class HandleManager {

  private final ConcurrentMap<Long, SomeHandle> refToHandle =
      new ConcurrentHashMap<Long, SomeHandle>();

  public void putAll(List<SomeHandle> list) {
    for (SomeHandle handle : list) {
      put(handle.handle(), handle);
    }
  }

  SomeHandle put(Long ref, JSONObject object) {
    SomeHandle smthWithHandle;
    try {
      smthWithHandle = V8ProtocolUtil.getV8Parser().parse(object, SomeHandle.class);
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }
    put(ref, smthWithHandle);
    return smthWithHandle;
  }

  private void put(Long ref, SomeHandle smthWithHandle) {
    SomeHandle oldObject = refToHandle.putIfAbsent(ref, smthWithHandle);
    if (oldObject != null) {
      mergeValues(oldObject, smthWithHandle);
    }
  }

  void put(SomeHandle someHandle) {
    if (someHandle.handle() >= 0) {
      put(someHandle.handle(), someHandle);
    }
  }

  public SomeHandle getHandle(Long ref) {
    return refToHandle.get(ref);
  }

  private static void mergeValues(SomeHandle oldObject, SomeHandle newObject) {
  }
}
