// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface BreakpointBody extends JsonSubtype<CommandResponseBody> {

  long breakpoint();

  @JsonOptionalField
  @JsonNullable
  Object column();

  @JsonOptionalField
  Long line();

  @JsonOptionalField
  String script_name();

  @JsonOptionalField
  String type();
}
