// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface BreakpointBody extends JsonSubtype<CommandResponseBody> {

  long breakpoint();

  @JsonOptionalField
  @JsonNullable
  Object column();

  @JsonOptionalField
  Long line();

  @JsonOptionalField
  String script_name();

  @JsonOptionalField
  String type();
}
