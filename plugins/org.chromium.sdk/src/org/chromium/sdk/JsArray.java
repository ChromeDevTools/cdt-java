// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

import java.util.SortedMap;

import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * Extends {@link JsObject} interface by adding methods for handling array elements.
 */
public interface JsArray extends JsObject {

  /**
   * @return the array length (index of the last element plus one),
   *         0 iff the array is empty
   * @throws MethodIsBlockingException because it may need to load value from remote
   */
  long getLength() throws MethodIsBlockingException;

  /**
   * @param index in the array
   * @return a {@code JsVariable} at the {@code index}, or {@code null} if there
   *         is no value at the specified index in the array
   * @throws MethodIsBlockingException because it may need to load value from remote
   */
  JsVariable get(long index) throws MethodIsBlockingException;

  /**
   * @return a map whose keys are array indices and values are {@code
   *         JsVariable} instances found at the corresponding indices. The
   *         resulting map is guaranteed to be sorted in the ascending key
   *         order.
   * @throws MethodIsBlockingException because it may need to load value from remote
   */
  SortedMap<Long, ? extends JsVariable> toSparseArray() throws MethodIsBlockingException;
}
