// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input.data;

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
