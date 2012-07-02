// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
