// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.input.data;

import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface BreakpointInfo {
  Type type();

  @JsonOptionalField
  String script_name();

  @JsonOptionalField
  Long script_id();

  @JsonOptionalField
  String script_regexp();

  long number();

  long line();

  Long column();

  Long groupId();

  long hit_count();

  boolean active();

  @JsonNullable
  String condition();

  long ignoreCount();

  enum Type {
    SCRIPTNAME,
    SCRIPTID,
    SCRIPTREGEXP,
    FUNCTION
  }
}
