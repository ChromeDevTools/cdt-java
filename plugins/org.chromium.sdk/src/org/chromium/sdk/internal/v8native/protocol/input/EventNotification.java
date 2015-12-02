// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import java.util.EnumSet;
import java.util.List;

import org.chromium.sdk.internal.protocolparser.EnumValueCondition;
import org.chromium.sdk.internal.protocolparser.JsonObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionCustom;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;
import org.json.simple.JSONObject;

/**
 * A type for event notification message. Its structure is similar
 * to {@link SuccessCommandResponse}.
 */
@JsonType
public interface EventNotification extends JsonObjectBased, JsonSubtype<IncomingMessage> {
  @JsonOverrideField
  @JsonSubtypeConditionCustom(condition=TypeValueCondition.class)
  MessageType type();

  class TypeValueCondition extends EnumValueCondition<MessageType> {
    public TypeValueCondition() {
      super(EnumSet.of(MessageType.EVENT));
    }
  }

  String event();

  EventNotificationBody body();

  // TODO(peter.rybin): does this field really exist?
  @JsonOptionalField
  JSONObject exception();

  @JsonOptionalField
  List<SomeHandle> refs();
}
