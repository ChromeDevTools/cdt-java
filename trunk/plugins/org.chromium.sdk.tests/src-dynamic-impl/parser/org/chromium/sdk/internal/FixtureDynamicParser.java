// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Arrays;
import java.util.Collections;

import org.chromium.sdk.internal.browserfixture.FixtureChromeStub.FixtureParser;
import org.chromium.sdk.internal.browserfixture.FixtureChromeStub.Refs;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.chromium.sdk.internal.v8native.protocol.input.V8DynamicParser;

/**
 * A dynamic implementation of a fixture parser.
 */
public class FixtureDynamicParser {
  public static DynamicParserImpl<FixtureParser> create() {
    try {
      return new DynamicParserImpl<FixtureParser>(FixtureParser.class, Arrays.asList(Refs.class),
          Collections.singletonList(V8DynamicParser.create()));
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
