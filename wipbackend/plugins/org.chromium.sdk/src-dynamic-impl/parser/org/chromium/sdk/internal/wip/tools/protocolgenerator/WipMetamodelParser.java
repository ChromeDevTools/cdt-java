// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.util.Arrays;
import java.util.Collections;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;

/**
 * Creates dynamic parser implementation.
 */
class WipMetamodelParser {
  static JsonProtocolParser get() {
    return PARSER;
  }

  private static final JsonProtocolParser PARSER;
  static {
    Class<?>[] classes = {
        WipMetamodel.Root.class,
        WipMetamodel.Domain.class,
        WipMetamodel.Command.class,
        WipMetamodel.Parameter.class,
        WipMetamodel.Event.class,
        WipMetamodel.StandaloneType.class,
        WipMetamodel.ObjectProperty.class,
        WipMetamodel.ArrayItemType.class,
    };

    try {
      PARSER = new DynamicParserImpl(Arrays.asList(classes),
          Collections.<DynamicParserImpl>emptyList(), true);
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException("Failed to build metamodel parser", e);
    }
  }
}
