// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.test;

/**
 * Initializes and holds a value. If there has been a problem with initialization, throws an
 * exception from getter.
 */
class InitializedValue<T> {
  private final T value;
  private final RuntimeException exception;

  InitializedValue(Initializer<T> initializer) {
    T value = null;
    RuntimeException exception = null;
    try {
      value = initializer.calculate();
    } catch (RuntimeException e) {
      exception = e;
    }
    this.value = value;
    this.exception = exception;
  }

  T get() {
    if (exception != null) {
      throw new RuntimeException("Value has not been properly initialized, see saved cause",
          exception);
    }
    return value;
  }

  interface Initializer<T> {
    T calculate();
  }
}
