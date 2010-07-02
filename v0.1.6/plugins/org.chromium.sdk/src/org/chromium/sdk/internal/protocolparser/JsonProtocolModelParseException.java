// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
