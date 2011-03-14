// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface GetPropertiesData extends JsonSubtype<WipCommandResponse.Data> {
  List<? extends Property> result();

  @JsonOptionalField
  Boolean isException();

  @JsonType
  interface Property {
    String name();
    ValueData value();
  }
}
