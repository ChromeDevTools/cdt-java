// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol.data;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface PropertyWithRef extends JsonSubtype<PropertyObject> {
  @JsonSubtypeCondition(fieldIsAbsent=true)
  @JsonOptionalField
  Void value();

  long ref();

  @JsonOptionalField
  Object attributes();

  @JsonOptionalField
  Long propertyType();
}
