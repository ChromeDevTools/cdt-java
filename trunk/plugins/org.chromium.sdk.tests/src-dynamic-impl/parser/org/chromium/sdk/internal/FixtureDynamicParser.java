// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Arrays;

import org.chromium.sdk.internal.FixtureChromeStub.Refs;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.chromium.sdk.internal.tools.v8.V8DynamicParser;

/**
 * A dynamic implementation of a fixture parser.
 */
public class FixtureDynamicParser {
  public static DynamicParserImpl get() {
    return fixtureParser;
  }

  private static final DynamicParserImpl fixtureParser;
  static {
    try {
      fixtureParser = new DynamicParserImpl(Arrays.asList(Refs.class),
          Arrays.asList(V8DynamicParser.get()));
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
