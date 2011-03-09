// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface RyndaScope {
  long hasChildren();

  @JsonOptionalField
  ValueData thisObject();

  String description();
  ValueData.Id objectId();

  @JsonOptionalField
  boolean isLocal();

  String type();
}
