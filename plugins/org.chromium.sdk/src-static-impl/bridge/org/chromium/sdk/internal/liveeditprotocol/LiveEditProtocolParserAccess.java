// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.liveeditprotocol;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;

/**
 * An accessor to generated implementation of a liveedit protocol parser.
 */
public class LiveEditProtocolParserAccess {
  private static final GeneratedLiveEditProtocolParser PARSER =
      new GeneratedLiveEditProtocolParser();

  public static JsonProtocolParser get() {
    return PARSER;
  }
}