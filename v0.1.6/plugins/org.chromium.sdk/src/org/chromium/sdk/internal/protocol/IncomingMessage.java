// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;

/**
 * A base type for all incoming message from debugger. There are 2 kinds of messages: response
 * to a command and event notification. All messages must have unique sequence id.
 */
@JsonType
public interface IncomingMessage {
  long getSeq();

  MessageType getType();

  @JsonSubtypeCasting
  CommandResponse asCommandResponse();

  @JsonSubtypeCasting
  EventNotification asEventNotification();
}
