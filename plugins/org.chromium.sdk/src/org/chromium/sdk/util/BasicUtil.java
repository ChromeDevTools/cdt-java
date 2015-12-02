// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Utilities for safe using collections and several small methods missing in standard Java library.
 */
public class BasicUtil {
  /**
   * Convenient method wrapping {@link Collection#toArray()}. It creates array of proper
   * length of proper type.
   */
  public static <T> T[] toArray(Collection<? extends T> collection, Class<T> clazz) {
    T[] result = (T[]) Array.newInstance(clazz, collection.size());
    collection.toArray(result);
    return result;
  }

  /**
   * Type-safe wrapper for {@link Map#remove(Object)} method. It restricts
   * type of key and makes sure that you do not try to remove key of wrong
   * type.
   */
  public static <K, V> V removeSafe(Map<K, V> map, K key) {
    return map.remove(key);
  }

  /**
   * Type-safe wrapper for {@link Map#get(Object)} method. It restricts
   * type of key and makes sure that you do not try to get by key of wrong
   * type.
   */
  public static <K, V> V getSafe(Map<K, V> map, K key) {
    return map.get(key);
  }

  /**
   * Type-safe wrapper for {@link Map#containsKey(Object)} method. It restricts
   * type of a value and makes sure that you do not call method for the value
   * wrong type.
   */
  public static <K, V> boolean containsKeySafe(Map<K, V> map, K key) {
    return map.containsKey(key);
  }

  /**
   * Type-safe wrapper for {@link Collection#contains(Object)} method. It restricts
   * type of a value and makes sure that you do not call method for the value
   * wrong type.
   */
  public static <V> boolean containsSafe(Collection<V> collection, V value) {
    return collection.contains(value);
  }

  /**
   * Type-safe wrapper for {@link Collection#remove(Object)} method. It restricts
   * type of a value and makes sure that you do not call method for the value
   * wrong type.
   */
  public static <V> boolean removeSafe(Collection<V> collection, V value) {
    return collection.remove(value);
  }

  /**
   * Convenience wrapper around {@link Object#equals(Object)} method that allows
   * both left and right to be null.
   */
  public static <T> boolean eq(T left, T right) {
    if (left == null) {
      return right == null;
    } else if (left == right) {
      return true;
    } else {
      return left.equals(right);
    }
  }

  /**
   * Convenience wrapper around {@link Object#hashCode()} method that allows
   * object to be null.
   */
  public static int hashCode(Object obj) {
    if (obj == null) {
      return 0;
    } else {
      return obj.hashCode();
    }
  }

  /**
   * Implementation of traditional join operation.
   */
  public static String join(Iterable<? extends String> components, String separator) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String c : components) {
      if (first) {
        first = false;
      } else {
        builder.append(separator);
      }
      builder.append(c);
    }
    return builder.toString();
  }

  /**
   * Takes stacktrace string out of exception.
   */
  public static String getStacktraceString(Exception exception) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    exception.printStackTrace(printWriter);
    printWriter.close();
    return stringWriter.toString();
  }
}
