// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol.data;

import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonType;

/**
 * A reference (a pointer) to an object, that prefetches some of its key properties.
 * <p>Gets serialized in mirror-delay.js,
 * JSONProtocolSerializer.prototype.serializeReferenceWithDisplayData_
 */
@JsonType
public interface RefWithDisplayData extends JsonSubtype<SomeRef> {

  @JsonOverrideField
  long ref();

  @JsonSubtypeCondition
  String type();

  @JsonOptionalField
  String className();

  @JsonOptionalField
  @JsonNullable
  Object value();


  // For function.
  @JsonOptionalField
  String inferredName();

  @JsonOptionalField
  Long scriptId();
}
