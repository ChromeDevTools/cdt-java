// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
