// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.Collections;

import org.chromium.sdk.internal.protocolparser.dynamicimpl.GeneratedCodeMap;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.ParserGeneratorBase;
import org.chromium.sdk.internal.tools.v8.liveedit.LiveEditParserGenerator;

/**
 * A main class that generates V8 protocol static parser implementation.
 */
public class V8ParserGenerator extends ParserGeneratorBase {
  public static void main(String[] args) {
    mainImpl(args, createConfiguration());
  }

  public static GenerateConfiguration createConfiguration() {
    GeneratedCodeMap liveEditParserMap =
        buildParserMap(LiveEditParserGenerator.createConfiguration());
    return new GenerateConfiguration("org.chromium.sdk.internal.protocol",
        "GeneratedV8ProtocolParser", V8DynamicParser.get(),
        Collections.singletonList(liveEditParserMap));
  }
}
