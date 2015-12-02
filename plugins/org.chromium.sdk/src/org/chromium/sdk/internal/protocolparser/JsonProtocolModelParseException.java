// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser;

/**
 * Signals that JSON model has some problem in it.
 */
public class JsonProtocolModelParseException extends Exception {
  public JsonProtocolModelParseException() {
    super();
  }
  public JsonProtocolModelParseException(String message, Throwable cause) {
    super(message, cause);
  }
  public JsonProtocolModelParseException(String message) {
    super(message);
  }
  public JsonProtocolModelParseException(Throwable cause) {
    super(cause);
  }
}
