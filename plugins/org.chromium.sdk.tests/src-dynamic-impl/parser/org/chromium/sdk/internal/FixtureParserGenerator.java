// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
        FixtureDynamicParser.create(),
        Collections.singletonList(baseV8ParserMap));
  }
}
