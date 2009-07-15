// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.SortedMap;

/**
 * This interface adds methods for handling array elements to the JsObject.
 */
public interface JsArray extends JsObject {

  /**
   * @return the index of the last element in the array or 0 if the array is
   *         empty
   */
  int length();

  /**
   * @param index in the array
   * @return a {@code JsVariable} at the {@code index}. {@code null} if there is
   *         no value at the specified index in the array
   */
  JsVariable get(int index);

  /**
   * @return a map whose keys are array indices and values are {@code
   *         JsVariable} instances found at the corresponding indices. The
   *         resulting map is guaranteed to be sorted in the ascending key
   *         order.
   */
  SortedMap<Integer, ? extends JsVariable> toSparseArray();
}
