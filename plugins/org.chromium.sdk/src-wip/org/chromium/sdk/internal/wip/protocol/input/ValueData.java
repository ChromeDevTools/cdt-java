// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ValueData {
  @JsonOptionalField
  Id objectId();

  String type();
  String description();

  // Either number or false or null.
  @JsonOptionalField
  Object hasChildren();

  @JsonType
  interface Id {
    long id();
    long injectedScriptId();

    @JsonOptionalField
    String groupName();
  }
}
