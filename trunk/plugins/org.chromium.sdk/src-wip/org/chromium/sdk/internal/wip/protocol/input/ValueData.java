// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ValueData {
  @JsonNullable
  Id objectId();

  String type();
  String description();

  // Either number or false or null.
  @JsonNullable
  Object hasChildren();

  @JsonType
  interface Id {
    long id();
    String groupName();
    long injectedScriptId();
  }
}
