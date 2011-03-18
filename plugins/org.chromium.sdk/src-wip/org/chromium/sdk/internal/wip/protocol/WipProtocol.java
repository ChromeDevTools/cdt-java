// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol;

import java.util.Arrays;
import java.util.Collections;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JsonProtocolParser;
import org.chromium.sdk.internal.wip.protocol.input.EvaluateData;
import org.chromium.sdk.internal.wip.protocol.input.GetPropertiesData;
import org.chromium.sdk.internal.wip.protocol.input.InspectedUrlChangedData;
import org.chromium.sdk.internal.wip.protocol.input.ParsedScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.input.PausedScriptData;
import org.chromium.sdk.internal.wip.protocol.input.WipCallFrame;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.input.WipEvent;
import org.chromium.sdk.internal.wip.protocol.input.WipScope;
import org.chromium.sdk.internal.wip.protocol.input.ScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.input.SetBreakpointData;
import org.chromium.sdk.internal.wip.protocol.input.ValueData;

/**
 * Main utility class of Wip protocol implementation.
 */
public class WipProtocol {
  public static final JsonProtocolParser PARSER;
  static {
    try {
      Class<?>[] jsonTypes = {
          WipEvent.class, WipEvent.Data.class,
          InspectedUrlChangedData.class,
          ParsedScriptSourceData.class,
          PausedScriptData.class, PausedScriptData.Details.class,
          WipCallFrame.class, WipCallFrame.Id.class,
          WipScope.class,
          WipCommandResponse.class, WipCommandResponse.Data.class,
          WipCommandResponse.Success.class, WipCommandResponse.Error.class,
          WipCommandResponse.Stub.class,
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
