// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
