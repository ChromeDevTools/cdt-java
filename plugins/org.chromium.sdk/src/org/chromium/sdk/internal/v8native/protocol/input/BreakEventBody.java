// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.json.simple.JSONObject;

@JsonType
public interface BreakEventBody extends JsonSubtype<EventNotificationBody> {

  @JsonOptionalField
  List<Long> breakpoints();

  @JsonOptionalField
  ValueHandle exception();

  @JsonOptionalField
  String sourceLineText();

  @JsonOptionalField
  Boolean uncaught();

  @JsonOptionalField
  Long sourceLine();

  @JsonOptionalField
  String invocationText();

  @JsonOptionalField
  JSONObject script();

  @JsonOptionalField
  Long sourceColumn();
}
