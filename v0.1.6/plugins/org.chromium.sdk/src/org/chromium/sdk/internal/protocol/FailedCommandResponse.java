// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionBoolValue;
import org.chromium.sdk.internal.protocolparser.JsonType;

/**
 * A type for failed command response message. It should contain "message" field
 * hinting at the cause of the failure.
 */
@JsonType
public interface FailedCommandResponse extends JsonSubtype<CommandResponse> {
  @JsonOverrideField
  @JsonSubtypeConditionBoolValue(false)
  boolean success();

  String getMessage();

  @JsonField(jsonLiteralName="request_seq")
  Long getRequestSeq();

  @JsonOptionalField
  String getCommand();
}
