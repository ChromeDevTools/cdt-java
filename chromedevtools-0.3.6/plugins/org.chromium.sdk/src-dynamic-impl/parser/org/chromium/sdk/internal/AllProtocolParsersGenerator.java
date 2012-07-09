// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.internal.liveeditprotocol.LiveEditParserGenerator;
import org.chromium.sdk.internal.shellprotocol.tools.protocol.input.ToolsProtocolParserGenerator;
import org.chromium.sdk.internal.v8native.protocol.input.V8ParserGenerator;

/**
 * A main class that generates all protocol static parsers (except tests).
 */
public class AllProtocolParsersGenerator {
  public static void main(String[] args) {
    LiveEditParserGenerator.main(args);
    ToolsProtocolParserGenerator.main(args);
    V8ParserGenerator.main(args);
  }
}
