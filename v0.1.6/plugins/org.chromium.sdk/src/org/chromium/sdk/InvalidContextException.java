// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * Signals that operation is not available because related {@link DebugContext}
 * is no more valid. However, there is no guarantee this exception will be thrown
 * in each case. Note also that {@link DebugContext#continueVm} throws
 * simple {@link IllegalStateException}.
 */
public class InvalidContextException extends RuntimeException {
  InvalidContextException() {
    super();
  }
  InvalidContextException(String message, Throwable cause) {
    super(message, cause);
  }
  InvalidContextException(String message) {
    super(message);
  }
  public InvalidContextException(Throwable cause) {
    super(cause);
  }
}
