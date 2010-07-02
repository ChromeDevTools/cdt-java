// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.system;

public class SmokeException extends Exception {
  public SmokeException() {
  }
  public SmokeException(String message, Throwable cause) {
    super(message, cause);
  }
  public SmokeException(String message) {
    super(message);
  }
  public SmokeException(Throwable cause) {
    super(cause);
  }
}
