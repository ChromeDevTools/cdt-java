// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
