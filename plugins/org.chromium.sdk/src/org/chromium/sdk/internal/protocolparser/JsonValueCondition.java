// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
