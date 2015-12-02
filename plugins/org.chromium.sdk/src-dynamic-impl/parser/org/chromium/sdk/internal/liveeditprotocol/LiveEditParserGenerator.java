// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.liveeditprotocol;

import org.chromium.sdk.internal.protocolparser.dynamicimpl.ParserGeneratorBase;

/**
 * A main class that generates V8 protocol static parser implementation.
 */
public class LiveEditParserGenerator extends ParserGeneratorBase {
  public static void main(String[] args) {
    mainImpl(args, createConfiguration());
  }

  public static GenerateConfiguration createConfiguration() {
    return new GenerateConfiguration("org.chromium.sdk.internal.liveeditprotocol",
        "GeneratedLiveEditProtocolParser", LiveEditDynamicParser.create());
  }
}
