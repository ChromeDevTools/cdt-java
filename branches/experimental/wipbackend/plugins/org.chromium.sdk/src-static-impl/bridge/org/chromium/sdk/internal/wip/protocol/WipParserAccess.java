// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;

/**
 * An accessor to generated implementation of a WIP parser.
 */
public class WipParserAccess {

  public static JsonProtocolParser get() {
    return PARSER;
  }

  private static final JsonProtocolParser PARSER = new GeneratedWipProtocolParser();

}
