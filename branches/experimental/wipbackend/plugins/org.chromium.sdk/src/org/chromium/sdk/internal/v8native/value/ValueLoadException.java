// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

/**
 * Signals a problem in loading value from remote, most probably a parsing problem.
 */
public class ValueLoadException extends RuntimeException {

  public ValueLoadException() {
  }

  public ValueLoadException(String message, Throwable cause) {
    super(message, cause);
  }

  public ValueLoadException(String message) {
    super(message);
  }

  public ValueLoadException(Throwable cause) {
    super(cause);
  }

}
