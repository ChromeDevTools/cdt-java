// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class that behaves similarly to ConcurrentHashMap, but also
 * provides {@link #close} operation that stops all new registrations.
 */
public class CloseableMap<K, V> {
  public static <K, V> CloseableMap<K, V> newMap() {
    return new CloseableMap<K, V>(new ConcurrentHashMap<K, V>());
  }

  public static <K, V> CloseableMap<K, V> newLinkedMap() {
    // Creates linked hash map and makes "get" synchronized.
    return new CloseableMap<K, V>(new LinkedHashMap<K, V>()) {
      @Override
      public synchronized V get(K key) {
        // Base "get" is not synchronized, because we rely on ConcurrentHashMap.
        return super.get(key);
      }
    };
  }

  private final Map<K, V> map;
  private boolean mutationClosed = false;

  protected CloseableMap(Map<K, V> map) {
    this.map = map;
  }

  public V get(K key) {
    return map.get(key);
  }

  public synchronized Map<K, V> close() {
    if (mutationClosed) {
      throw new IllegalStateException();
    }
    mutationClosed = true;
    return map;
  }

  public synchronized V remove(K key) {
    if (mutationClosed) {
      // We probably can safely ignore this.
      return null;
    }
    V result = map.remove(key);
    if (result == null) {
      throw new IllegalArgumentException("This key is not registered");
    }
    return result;
  }

  public synchronized V removeIfContains(K key) {
    if (mutationClosed) {
      // We probably can safely ignore this.
      return null;
    }
    return map.remove(key);
  }

  public synchronized void put(K key, V value) {
    if (mutationClosed) {
      throw new IllegalStateException();
    }
    if (map.containsKey(key)) {
      throw new IllegalStateException("Such key is already registered");
    }
    map.put(key, value);
  }

  public int size() {
    return map.size();
  }

  /**
   * @return a first value in (ordered) map or null if map is empty
   */
  public synchronized V peekFirst() {
    if (map.isEmpty()) {
      return null;
    }
    return map.values().iterator().next();
  }
}
