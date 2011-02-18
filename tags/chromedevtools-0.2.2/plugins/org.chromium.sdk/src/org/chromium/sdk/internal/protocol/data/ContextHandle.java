// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol.data;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ContextHandle extends JsonSubtype<SomeHandle> {
  /**
   * Any value, provided by V8 embedding application. For Chrome it may be {@link ContextData}
   * as well as its stringified form (as "type,in").
   */
  @JsonOptionalField
  Object data();
}
