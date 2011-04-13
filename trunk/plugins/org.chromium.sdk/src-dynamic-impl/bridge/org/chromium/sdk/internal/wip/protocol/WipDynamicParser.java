// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol;

import java.util.Arrays;
import java.util.Collections;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.chromium.sdk.internal.wip.protocol.input.EvaluateData;
import org.chromium.sdk.internal.wip.protocol.input.GetPropertiesData;
import org.chromium.sdk.internal.wip.protocol.input.InspectedUrlChangedData;
import org.chromium.sdk.internal.wip.protocol.input.ParsedScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.input.PausedScriptData;
import org.chromium.sdk.internal.wip.protocol.input.ScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.input.SetBreakpointData;
import org.chromium.sdk.internal.wip.protocol.input.ValueData;
import org.chromium.sdk.internal.wip.protocol.input.WipCallFrame;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.input.WipEvent;
import org.chromium.sdk.internal.wip.protocol.input.WipScope;
import org.chromium.sdk.internal.wip.protocol.input.WipTabList;

/**
 * A dynamic implementation of a WebInspector protocol parser.
 */
public class WipDynamicParser {
  public static DynamicParserImpl get() {
    return DYNAMIC_IMPLEMENTATION;
  }

  private static final DynamicParserImpl DYNAMIC_IMPLEMENTATION;

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
          SetBreakpointData.class,

          // Tab list protocol interfaces.
          WipTabList.class, WipTabList.TabDescription.class
      };
      DYNAMIC_IMPLEMENTATION = new DynamicParserImpl(Arrays.asList(jsonTypes),
          Collections.<DynamicParserImpl>emptyList(), true);
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
