// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser;

import java.util.EnumSet;
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
  protected EnumValueCondition(T singleAllowedValues) {
    this.allowedValues = EnumSet.<T>of(singleAllowedValues);
  }

  public boolean conforms(T value) {
    return allowedValues.contains(value);
  }

  public static String decorateEnumConstantName(String enumValue) {
    return enumValue.toUpperCase().replace("-", "_");
  }
}
