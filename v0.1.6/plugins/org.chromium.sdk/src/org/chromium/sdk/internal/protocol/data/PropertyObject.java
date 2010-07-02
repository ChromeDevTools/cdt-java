// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol.data;

import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;

/**
 * A type for a property object. May have 2 different forms (subtypes).
 * <p>Gets serialized in mirror-delay.js,
 * JSONProtocolSerializer.prototype.serializeProperty_
 */
@JsonType
public interface PropertyObject {
  /**
   * @return either String (normal property) or Long (array element)
   */
  Object name();

  @JsonSubtypeCasting
  PropertyWithValue asPropertyWithValue();

  @JsonSubtypeCasting
  PropertyWithRef asPropertyWithRef();
}
