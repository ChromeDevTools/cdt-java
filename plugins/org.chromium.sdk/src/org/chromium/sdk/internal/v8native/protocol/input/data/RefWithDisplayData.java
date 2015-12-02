// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input.data;

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
