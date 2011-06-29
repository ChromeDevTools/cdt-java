// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.shellprotocol.tools;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.Message.Header;

/**
 * A facility that creates transport {@link Message}s for sending requests to
 * Chromium using the available ChromeDevTools Protocol commands.
 */
public class MessageFactory {
  public static Message createMessage(String tool, String destination, String content) {
    Map<String, String> headers = new HashMap<String, String>();
    if (tool != null) {
      headers.put(Header.TOOL.name, tool);
    }
    if (destination != null) {
      headers.put(Header.DESTINATION.name, destination);
    }
    return new Message(headers, content);
  }

  private MessageFactory() {
    // not instantiable
  }
}
