// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal;

import org.chromium.sdk.internal.liveeditprotocol.LiveEditParserGenerator;
import org.chromium.sdk.internal.v8native.protocol.input.V8ParserGenerator;

/**
 * A main class that generates all protocol static parsers (except tests).
 */
public class AllProtocolParsersGenerator {
  public static void main(String[] args) {
    LiveEditParserGenerator.main(args);
    V8ParserGenerator.main(args);
  }
}
