// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.rynda.protocol.BasicConstants;

@JsonType
public interface RyndaEvent {
  @JsonField(jsonLiteralName = BasicConstants.Property.EVENT)
  String event();

  @JsonField(jsonLiteralName = BasicConstants.Property.DOMAIN)
  String domain();

  String type();

  @JsonOptionalField
  Data data();

  @JsonType(subtypesChosenManually=true)
  interface Data {
    @JsonSubtypeCasting
    InspectedUrlChangedData asInspectedUrlChangedData() throws JsonProtocolParseException;

    @JsonSubtypeCasting
    ParsedScriptSourceData asParsedScriptSourceData() throws JsonProtocolParseException;

    @JsonSubtypeCasting
    PausedScriptData asPausedScriptData() throws JsonProtocolParseException;
  }
}
