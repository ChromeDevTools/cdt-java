// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import java.util.EnumSet;

import org.chromium.sdk.internal.protocolparser.EnumValueCondition;
import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionCustom;
import org.chromium.sdk.internal.protocolparser.JsonType;

/**
 * A generic type for all command responses. There are 2 subtypes; one for
 * success responses and one for failure responses.
 */
@JsonType
public interface CommandResponse extends JsonSubtype<IncomingMessage> {

  @JsonOverrideField
  @JsonSubtypeConditionCustom(condition=TypeValueCondition.class)
  MessageType type();

  class TypeValueCondition extends EnumValueCondition<MessageType> {
    public TypeValueCondition() {
      super(EnumSet.of(MessageType.RESPONSE));
    }
  }

  /**
   * Id of the corresponding request sent to debugger.
   */
  @JsonField(jsonLiteralName="request_seq")
  long requestSeq();

  @JsonOptionalField
  String command();

  boolean success();

  @JsonSubtypeCasting
  SuccessCommandResponse asSuccess();

  @JsonSubtypeCasting
  FailedCommandResponse asFailure();
}
