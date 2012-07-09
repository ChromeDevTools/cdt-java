// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.input.data;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.v8native.protocol.input.FrameObject;

/**
 * A serialized form of object when it is fully (though shallowly) described. Object always
 * has a type and a handle. (See {@link FrameObject} as a case that makes it a bit more messy).
 * <p>Gets serialized in mirror-delay.js,
 * JSONProtocolSerializer.prototype.serialize_, main part.
 */
@JsonType(subtypesChosenManually=true)
public interface SomeHandle extends JsonSubtype<SomeSerialized> {
  /**
   * An integer "handle" of the object. Normally it is unique (for particular suspended-to-resumed
   * period). Some auxiliary objects may have non-unique handles which should be negative.
   */
  @JsonSubtypeCondition
  long handle();

  String type();


  @JsonSubtypeCasting
  ScriptHandle asScriptHandle() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  ValueHandle asValueHandle() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  ContextHandle asContextHandle() throws JsonProtocolParseException;
}
