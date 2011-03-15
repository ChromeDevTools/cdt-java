// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionBoolValue;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.wip.protocol.BasicConstants;

@JsonType
public interface WipCommandResponse extends JsonObjectBased {
  @JsonField(jsonLiteralName = BasicConstants.Property.SEQ)
  Object seq();

  @JsonSubtypeCasting Success asSuccess();
  @JsonSubtypeCasting Error asError();

  @JsonType
  interface Success extends JsonSubtype<WipCommandResponse> {
    @JsonField(jsonLiteralName = BasicConstants.Property.DOMAIN)
    @JsonOptionalField
    String domain();

    @JsonSubtypeCondition(fieldIsAbsent=true)
    @JsonOptionalField
    List<String> errors();

    @JsonField(jsonLiteralName="body")
    @JsonOptionalField
    Data data();

    @JsonSubtypeConditionBoolValue(value = true)
    boolean success();
  }

  @JsonType
  interface Error extends JsonSubtype<WipCommandResponse> {
    @JsonOverrideField
    @JsonSubtypeCondition()
    List<String> errors();

    @JsonField(jsonLiteralName = BasicConstants.Property.DOMAIN)
    @JsonOptionalField
    String domain();

    @JsonSubtypeConditionBoolValue(value = false)
    boolean success();
  }

  @JsonType(subtypesChosenManually=true)
  interface Data {
    @JsonSubtypeCasting ScriptSourceData asScriptSourceData() throws JsonProtocolParseException;
    @JsonSubtypeCasting EvaluateData asEvaluateData() throws JsonProtocolParseException;
    @JsonSubtypeCasting GetPropertiesData asGetPropertiesData() throws JsonProtocolParseException;
    @JsonSubtypeCasting SetBreakpointData asSetBreakpointData() throws JsonProtocolParseException;
  }
}
