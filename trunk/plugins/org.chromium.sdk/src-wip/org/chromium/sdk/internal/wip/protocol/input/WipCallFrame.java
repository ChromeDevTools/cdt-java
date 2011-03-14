// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface WipCallFrame {
  Long sourceID();
  Id id();
  List<WipScope> scopeChain();

  // 1-based?
  long line();

  // 1-based?
  long column();

  String functionName();
  String type();

  @JsonType
  interface Id {
    long ordinal();
    long injectedScriptId();
  }
}
