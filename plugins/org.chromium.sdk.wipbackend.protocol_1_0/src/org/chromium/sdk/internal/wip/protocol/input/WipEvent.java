// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface WipEvent {
  String method();

  @JsonField(jsonLiteralName = "params")
  @JsonOptionalField
  Data data();

  @JsonType(subtypesChosenManually=true, allowsOtherProperties=true)
  interface Data extends JsonObjectBased {
  }
}
