// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.shellprotocol.tools.protocol.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;

/**
 * Dynamic implementation of tools protocol parser.
 */
public class ToolsProtocolParser {
  public static JsonProtocolParser get() {
    return PARSER;
  }

  private static final DynamicParserImpl PARSER;
  static {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(ToolsMessage.class);
    classes.add(ToolsMessage.Data.class);

    try {
      PARSER = new DynamicParserImpl(classes, Collections.<DynamicParserImpl>emptyList(), false);
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
