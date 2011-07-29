// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
