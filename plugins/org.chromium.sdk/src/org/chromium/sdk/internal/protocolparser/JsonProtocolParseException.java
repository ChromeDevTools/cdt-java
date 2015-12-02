// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser;

/**
 * Signals a failure during JSON object parsing.
 */
public class JsonProtocolParseException extends Exception {
  public JsonProtocolParseException() {
    super();
  }
  public JsonProtocolParseException(String message, Throwable cause) {
    super(message, cause);
  }
  public JsonProtocolParseException(String message) {
    super(message);
  }
  public JsonProtocolParseException(Throwable cause) {
    super(cause);
  }
}
