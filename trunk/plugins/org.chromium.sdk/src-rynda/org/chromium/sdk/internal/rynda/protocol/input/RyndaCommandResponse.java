// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionBoolValue;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.rynda.protocol.BasicConstants;

@JsonType
public interface RyndaCommandResponse extends JsonObjectBased {
  @JsonField(jsonLiteralName = BasicConstants.Property.SEQ)
  Object seq();

  boolean success();

  @JsonSubtypeCasting Success asSuccess();
  @JsonSubtypeCasting Error asError();

  @JsonType
  interface Success extends JsonSubtype<RyndaCommandResponse> {
    @JsonOverrideField
    @JsonSubtypeConditionBoolValue(true)
    boolean success();

    @JsonOptionalField
    Data data();

    @JsonField(jsonLiteralName = BasicConstants.Property.DOMAIN)
    String domain();
  }

  @JsonType
  interface Error extends JsonSubtype<RyndaCommandResponse> {
    @JsonOverrideField
    @JsonSubtypeConditionBoolValue(false)
    boolean success();

    List<String> errors();

    @JsonField(jsonLiteralName = BasicConstants.Property.DOMAIN)
    @JsonOptionalField
    String domain();
  }


  @JsonType(subtypesChosenManually=true)
  interface Data {
    @JsonSubtypeCasting ScriptSourceData asScriptSourceData() throws JsonProtocolParseException;
    @JsonSubtypeCasting EvaluateData asEvaluateData() throws JsonProtocolParseException;
    @JsonSubtypeCasting GetPropertiesData asGetPropertiesData() throws JsonProtocolParseException;
    @JsonSubtypeCasting SetBreakpointData asSetBreakpointData() throws JsonProtocolParseException;
  }
}
