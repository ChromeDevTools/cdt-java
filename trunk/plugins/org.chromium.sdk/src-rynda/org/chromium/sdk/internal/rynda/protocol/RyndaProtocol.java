// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol;

import java.util.Arrays;
import java.util.Collections;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JsonProtocolParser;
import org.chromium.sdk.internal.rynda.protocol.input.EvaluateData;
import org.chromium.sdk.internal.rynda.protocol.input.GetPropertiesData;
import org.chromium.sdk.internal.rynda.protocol.input.InspectedUrlChangedData;
import org.chromium.sdk.internal.rynda.protocol.input.ParsedScriptSourceData;
import org.chromium.sdk.internal.rynda.protocol.input.PausedScriptData;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaCallFrame;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaCommandResponse;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaEvent;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaScope;
import org.chromium.sdk.internal.rynda.protocol.input.ScriptSourceData;
import org.chromium.sdk.internal.rynda.protocol.input.SetBreakpointData;
import org.chromium.sdk.internal.rynda.protocol.input.ValueData;

/**
 * Main utility class of Rynda protocol implementation.
 */
public class RyndaProtocol {
  public static final JsonProtocolParser PARSER;
  static {
    try {
      Class<?>[] jsonTypes = {
          RyndaEvent.class, RyndaEvent.Data.class,
          InspectedUrlChangedData.class,
          ParsedScriptSourceData.class,
          PausedScriptData.class, PausedScriptData.Details.class,
          RyndaCallFrame.class, RyndaCallFrame.Id.class,
          RyndaScope.class,
          RyndaCommandResponse.class, RyndaCommandResponse.Data.class,
          RyndaCommandResponse.Success.class, RyndaCommandResponse.Error.class,
          ScriptSourceData.class,
          EvaluateData.class,
          ValueData.class, ValueData.Id.class,
          GetPropertiesData.class, GetPropertiesData.Property.class,
          SetBreakpointData.class
      };
      PARSER = new JsonProtocolParser(Arrays.asList(jsonTypes),
          Collections.<JsonProtocolParser>emptyList(), true);
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static int parseInt(Object obj) {
    if (obj instanceof String) {
      String str = (String) obj;
      float f = Float.parseFloat(str);
      return Math.round(f);
    } else if (obj instanceof Number) {
      Number number = (Number) obj;
      return number.intValue();
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static long parseSourceId(String value) {
    return Long.parseLong(value);
  }

  public static boolean parseHasChildren(Object hasChildren) {
    return hasChildren != Boolean.FALSE;
  }
}
