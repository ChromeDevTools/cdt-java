// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal;

/**
 * Signals incorrect (or unexpected) JSON content.
 */
public class JsonException extends RuntimeException {

  JsonException() {
  }

  JsonException(String message, Throwable cause) {
    super(message, cause);
  }

  JsonException(String message) {
    super(message);
  }

  JsonException(Throwable cause) {
    super(cause);
  }

}
