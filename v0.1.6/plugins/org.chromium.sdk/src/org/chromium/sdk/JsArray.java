// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.SortedMap;

import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * This interface adds methods for handling array elements to the JsObject.
 */
public interface JsArray extends JsObject {

  /**
   * @return the array length (index of the last element plus one),
   *         0 iff the array is empty
   */
  int length();

  /**
   * @param index in the array
   * @return a {@code JsVariable} at the {@code index}, or {@code null} if there
   *         is no value at the specified index in the array
   * @throws MethodIsBlockingException if called from a callback because it may
   *         need to load element data from remote
   */
  JsVariable get(int index) throws MethodIsBlockingException;

  /**
   * @return a map whose keys are array indices and values are {@code
   *         JsVariable} instances found at the corresponding indices. The
   *         resulting map is guaranteed to be sorted in the ascending key
   *         order.
   * @throws MethodIsBlockingException if called from a callback because
   *         the method needs all elements loaded and might block until
   *         it's done
   */
  SortedMap<Integer, ? extends JsVariable> toSparseArray() throws MethodIsBlockingException;
}
