// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.wip.protocol.BasicConstants;

@JsonType
public interface WipCommandResponse extends JsonObjectBased {
  @JsonField(jsonLiteralName = BasicConstants.Property.ID)
  Long id();

  @JsonSubtypeCasting Success asSuccess();
  @JsonSubtypeCasting Error asError();

  @JsonType
  interface Success extends JsonSubtype<WipCommandResponse> {
    @JsonSubtypeCondition(fieldIsAbsent=true)
    @JsonOptionalField
    Void error();

    @JsonField(jsonLiteralName="result")
    @JsonSubtypeCondition
    Data data();
  }

  @JsonType
  interface Error extends JsonSubtype<WipCommandResponse> {
    @JsonOverrideField
    @JsonSubtypeCondition()
    ErrorInfo error();

    @JsonSubtypeCondition(fieldIsAbsent=true)
    @JsonField(jsonLiteralName="result")
    @JsonOptionalField
    Data data();

    @JsonType
    interface ErrorInfo {
      String message();
      List<String> data();
      long code();
    }
  }

  @JsonType(subtypesChosenManually=true, allowsOtherProperties=true)
  interface Data extends JsonObjectBased {
  }
}
