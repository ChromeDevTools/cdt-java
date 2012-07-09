// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
