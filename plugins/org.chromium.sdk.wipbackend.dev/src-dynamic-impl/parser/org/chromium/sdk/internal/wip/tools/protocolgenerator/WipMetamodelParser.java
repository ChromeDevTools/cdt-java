// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.util.Arrays;
import java.util.Collections;

import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.Root;

/**
 * Parser for WIP JSON metamodel.
 */
@JsonParserRoot
interface WipMetamodelParser {

  @JsonParseMethod
  Root parseRoot(Object jsonValue) throws JsonProtocolParseException;

  /**
   * Creates dynamic parser implementation.
   */
  class Impl {
    static WipMetamodelParser get() {
      return INTSTANCE;
    }

    private static final WipMetamodelParser INTSTANCE;
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

      DynamicParserImpl<WipMetamodelParser> dynamicParserImpl;
      try {
        dynamicParserImpl = new DynamicParserImpl<WipMetamodelParser>(WipMetamodelParser.class,
            Arrays.asList(classes), Collections.<DynamicParserImpl<?>>emptyList(), true);
      } catch (JsonProtocolModelParseException e) {
        throw new RuntimeException("Failed to build metamodel parser", e);
      }
      INTSTANCE = dynamicParserImpl.getParserRoot();
    }
  }
}
