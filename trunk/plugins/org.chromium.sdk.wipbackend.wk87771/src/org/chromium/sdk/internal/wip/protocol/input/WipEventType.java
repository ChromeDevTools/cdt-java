// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.json.simple.JSONObject;

/**
 * This class describes event static information that helps to automate response parsing
 * and dispatching.
 */
public abstract class WipEventType<T> {
  private final String methodName;
  private final Class<T> eventType;

  public WipEventType(String methodName, Class<T> eventType) {
    this.methodName = methodName;
    this.eventType = eventType;
  }

  public String getMethodName() {
    return methodName;
  }

  public Class<T> getEventType() {
    return eventType;
  }

  public abstract T parse(WipGeneratedParserRoot parser, JSONObject obj)
      throws JsonProtocolParseException;
}
