// Copyright (c) 2011 The Chromium Authors. All rights reserved.
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
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.rynda.protocol.BasicConstants;

@JsonType
public interface RyndaCommandResponse extends JsonObjectBased {
  @JsonField(jsonLiteralName = BasicConstants.Property.SEQ)
  Object seq();

  @JsonSubtypeCasting Success asSuccess();
  @JsonSubtypeCasting Error asError();
  @JsonSubtypeCasting Stub asStub();

  @JsonType
  interface Success extends JsonSubtype<RyndaCommandResponse> {
    @JsonSubtypeCondition(fieldIsAbsent=true)
    @JsonOptionalField
    List<String> errors();

    @JsonField(jsonLiteralName="body")
    @JsonSubtypeCondition
    Data data();
  }

  @JsonType
  interface Error extends JsonSubtype<RyndaCommandResponse> {
    @JsonOverrideField
    @JsonSubtypeCondition()
    List<String> errors();

    @JsonField(jsonLiteralName = BasicConstants.Property.DOMAIN)
    @JsonOptionalField
    String domain();
  }

  /**
   * A no-data type of response containing only "seq" property.
   */
  @JsonType
  interface Stub extends JsonSubtype<RyndaCommandResponse> {
    @JsonOverrideField
    @JsonSubtypeCondition(fieldIsAbsent = true)
    @JsonOptionalField
    List<String> errors();

    @JsonField(jsonLiteralName="body")
    @JsonSubtypeCondition(fieldIsAbsent = true)
    @JsonOptionalField
    Data data();
  }


  @JsonType(subtypesChosenManually=true)
  interface Data {
    @JsonSubtypeCasting ScriptSourceData asScriptSourceData() throws JsonProtocolParseException;
    @JsonSubtypeCasting EvaluateData asEvaluateData() throws JsonProtocolParseException;
    @JsonSubtypeCasting GetPropertiesData asGetPropertiesData() throws JsonProtocolParseException;
    @JsonSubtypeCasting SetBreakpointData asSetBreakpointData() throws JsonProtocolParseException;
  }
}
