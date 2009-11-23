// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import java.util.List;

import org.chromium.sdk.internal.protocol.data.ValueHandle;
import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.json.simple.JSONObject;

@JsonType
public interface BreakEventBody extends JsonSubtype<EventNotificationBody> {

  @JsonOptionalField
  List<Long> getBreakpoints();

  @JsonOptionalField
  ValueHandle getException();

  @JsonOptionalField
  String getSourceLineText();

  @JsonOptionalField
  @JsonField(jsonLiteralName="uncaught")
  Boolean isUncaught();

  @JsonOptionalField
  Long getSourceLine();

  @JsonOptionalField
  String getInvocationText();

  @JsonOptionalField
  JSONObject getScript();

  @JsonOptionalField
  Long getSourceColumn();
}
