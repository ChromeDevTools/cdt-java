// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;

/**
 * A map for storage and retrieval of special type handles, those not covered
 * by {@link ValueMirror} class. Special types are defined in {@link #isSpecialType(String)}
 * methods.
 */
public class HandleManager {

  private static final String SCRIPT_TYPE = "script";
  private static final String CONTEXT_TYPE = "context";

  public static boolean isSpecialType(String type) {
    return SCRIPT_TYPE.equals(type) || CONTEXT_TYPE.equals(type);
  }

  private final ConcurrentMap<Long, SomeHandle> refToHandle =
      new ConcurrentHashMap<Long, SomeHandle>();

  void put(Long ref, SomeHandle smthWithHandle) {
    SomeHandle oldObject = refToHandle.putIfAbsent(ref, smthWithHandle);
    if (oldObject != null) {
      mergeValues(oldObject, smthWithHandle);
    }
  }

  public SomeHandle getHandle(Long ref) {
    return refToHandle.get(ref);
  }

  private static void mergeValues(SomeHandle oldObject, SomeHandle newObject) {
  }
}
