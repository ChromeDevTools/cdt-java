// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input.data;

import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;


/**
 * A serialized form of object. There may be 2 schemas: reference (like pointer) or full description
 * called "a handle" hereafter. It appears that it's not always statically known which of schemas
 * is used in every place; thus it requires a base type like this.
 * <p>Gets serialized in mirror-delay.js,
 * JSONProtocolSerializer.prototype.serialize_
 */
@JsonType
public interface SomeSerialized {
  @JsonSubtypeCasting
  SomeRef asSomeRef();

  @JsonSubtypeCasting
  SomeHandle asSmthWithHandle();
}
