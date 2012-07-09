// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.input.data;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;


/**
 * A serialization of a JavaScript value. May be cast to {@link ObjectValueHandle} if value is
 * an object.
 * <p>Gets serialized in mirror-delay.js,
 * JSONProtocolSerializer.prototype.serialize_, main part
 */
@JsonType
public interface ValueHandle extends JsonSubtype<SomeHandle>  {
  @JsonOverrideField
  long handle();

  String text();

  @JsonOptionalField
  Object value();

  @JsonOverrideField
  String type();

  // for string type (the true length, value field may be truncated)
  @JsonOptionalField
  Long length();
  @JsonOptionalField
  Long fromIndex();
  @JsonOptionalField
  Long toIndex();

  @JsonOptionalField
  String className();

  @JsonSubtypeCasting
  ObjectValueHandle asObject();

  @JsonSubtypeCasting
  void asNotObject();
}

