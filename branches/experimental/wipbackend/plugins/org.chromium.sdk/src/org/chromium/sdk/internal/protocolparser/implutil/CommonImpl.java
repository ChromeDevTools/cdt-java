// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.implutil;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;

/**
 * Methods and classes commonly used by dynamic and generated implementations of
 * {@link JsonProtocolParser}.
 */
public class CommonImpl {

  public static class ParseRuntimeException extends RuntimeException {
    public ParseRuntimeException() {
    }
    public ParseRuntimeException(String message, Throwable cause) {
      super(message, cause);
    }
    public ParseRuntimeException(String message) {
      super(message);
    }
    public ParseRuntimeException(Throwable cause) {
      super(cause);
    }
  }

}
