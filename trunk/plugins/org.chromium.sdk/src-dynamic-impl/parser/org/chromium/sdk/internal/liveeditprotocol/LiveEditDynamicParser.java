// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.liveeditprotocol;

import org.chromium.sdk.internal.liveeditprotocol.LiveEditResult;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;

/**
 * A dynamic implementation of a v8 protocol parser.
 */
public class LiveEditDynamicParser {
  public static DynamicParserImpl get() {
    return parser;
  }

  private static final DynamicParserImpl parser;
  static {
    try {
      parser = new DynamicParserImpl(new Class<?>[] {
          LiveEditResult.class,
          LiveEditResult.OldTreeNode.class,
          LiveEditResult.NewTreeNode.class,
          LiveEditResult.Positions.class,
          LiveEditResult.TextualDiff.class,
      });
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
