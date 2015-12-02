// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionBoolValue;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;

/**
 * A type for success command response message. It holds all the data in
 * "body" field and usually provides "reference" part with data for all referenced objects.
 */
@JsonType
public interface SuccessCommandResponse extends JsonSubtype<CommandResponse> {
  @JsonOverrideField
  @JsonSubtypeConditionBoolValue(true)
  boolean success();

  @JsonOptionalField
  CommandResponseBody body();

  @JsonOptionalField
  List<SomeHandle> refs();

  /**
   * @return whether VM continue running after handling the command; however next commands
   *         may change it
   */
  boolean running();
}
