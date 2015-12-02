// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.liveeditprotocol;

import java.util.Arrays;
import java.util.Collections;

import org.chromium.sdk.internal.liveeditprotocol.LiveEditResult;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;

/**
 * A dynamic implementation of a v8 protocol parser.
 */
public class LiveEditDynamicParser {
  public static DynamicParserImpl<LiveEditProtocolParser> create() {
    try {
      return new DynamicParserImpl<LiveEditProtocolParser>(LiveEditProtocolParser.class,
          Arrays.asList(new Class<?>[] {
              LiveEditResult.class,
              LiveEditResult.OldTreeNode.class,
              LiveEditResult.NewTreeNode.class,
              LiveEditResult.Positions.class,
              LiveEditResult.TextualDiff.class,
              }),
          Collections.<DynamicParserImpl<?>>emptyList(), false);
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
