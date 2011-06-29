// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collections;

import org.chromium.sdk.internal.protocolparser.dynamicimpl.GeneratedCodeMap;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.ParserGeneratorBase;
import org.chromium.sdk.internal.v8native.protocol.input.V8ParserGenerator;

/**
 * A main class that generates test Fixture static parser implementation.
 */
public class FixtureParserGenerator extends ParserGeneratorBase {

  public static void main(String[] args) {
    mainImpl(args, createConfiguration());
  }

  public static GenerateConfiguration createConfiguration() {
    GeneratedCodeMap baseV8ParserMap = buildParserMap(V8ParserGenerator.createConfiguration());
    return new GenerateConfiguration("org.chromium.sdk.internal", "GeneratedV8FixtureParser",
        FixtureDynamicParser.get(),
        Collections.singletonList(baseV8ParserMap));
  }
}
