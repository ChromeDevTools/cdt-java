// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import java.util.Set;

/**
 * Implementation of {@link JsonValueCondition} for enum-typed values.
 * User is supposed to subclass it and specify allowed enum constants in constructor.
 * @param <T> type of value
 */
public abstract class EnumValueCondition<T extends Enum<T>> implements JsonValueCondition<T> {
  private final Set<T> allowedValues;
  protected EnumValueCondition(Set<T> allowedValues) {
    this.allowedValues = allowedValues;
  }

  public boolean conforms(T value) {
    return allowedValues.contains(value);
  }

  public static String decorateEnumConstantName(String enumValue) {
    return enumValue.toUpperCase().replace("-", "_");
  }
}
