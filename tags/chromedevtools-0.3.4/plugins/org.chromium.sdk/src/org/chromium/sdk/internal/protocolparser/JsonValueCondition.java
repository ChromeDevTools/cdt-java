// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

/**
 * A condition for property value. Implementation may provide any logic here.
 * @param <T> type of value
 * @see JsonSubtypeConditionCustom
 */
public interface JsonValueCondition<T> {
  /**
   * @param value parsed data from JSON property
   * @return true if value satisfies condition
   */
  boolean conforms(T value);
}
