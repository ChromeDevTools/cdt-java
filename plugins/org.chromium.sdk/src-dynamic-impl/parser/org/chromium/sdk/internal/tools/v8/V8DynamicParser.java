// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.Arrays;
import java.util.List;

import org.chromium.sdk.internal.protocol.AfterCompileBody;
import org.chromium.sdk.internal.protocol.BacktraceCommandBody;
import org.chromium.sdk.internal.protocol.BreakEventBody;
import org.chromium.sdk.internal.protocol.BreakpointBody;
import org.chromium.sdk.internal.protocol.ChangeLiveBody;
import org.chromium.sdk.internal.protocol.CommandResponse;
import org.chromium.sdk.internal.protocol.CommandResponseBody;
import org.chromium.sdk.internal.protocol.EventNotification;
import org.chromium.sdk.internal.protocol.EventNotificationBody;
import org.chromium.sdk.internal.protocol.FailedCommandResponse;
import org.chromium.sdk.internal.protocol.FlagsBody;
import org.chromium.sdk.internal.protocol.FrameObject;
import org.chromium.sdk.internal.protocol.IncomingMessage;
import org.chromium.sdk.internal.protocol.ListBreakpointsBody;
import org.chromium.sdk.internal.protocol.ScopeBody;
import org.chromium.sdk.internal.protocol.ScopeRef;
import org.chromium.sdk.internal.protocol.ScriptCollectedBody;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.protocol.VersionBody;
import org.chromium.sdk.internal.protocol.data.BreakpointInfo;
import org.chromium.sdk.internal.protocol.data.ContextData;
import org.chromium.sdk.internal.protocol.data.ContextHandle;
import org.chromium.sdk.internal.protocol.data.FunctionValueHandle;
import org.chromium.sdk.internal.protocol.data.ObjectValueHandle;
import org.chromium.sdk.internal.protocol.data.PropertyObject;
import org.chromium.sdk.internal.protocol.data.PropertyWithRef;
import org.chromium.sdk.internal.protocol.data.PropertyWithValue;
import org.chromium.sdk.internal.protocol.data.RefWithDisplayData;
import org.chromium.sdk.internal.protocol.data.ScriptHandle;
import org.chromium.sdk.internal.protocol.data.ScriptWithId;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocol.data.SomeRef;
import org.chromium.sdk.internal.protocol.data.SomeSerialized;
import org.chromium.sdk.internal.protocol.data.ValueHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.chromium.sdk.internal.tools.v8.liveedit.LiveEditDynamicParser;

/**
 * A dynamic implementation of a v8 protocol parser.
 */
public class V8DynamicParser {
  public static DynamicParserImpl get() {
    return parser;
  }

  private static final DynamicParserImpl parser;
  static {
    try {
      List<Class<?>> interfaces = Arrays.asList(
          IncomingMessage.class,
          EventNotification.class,
          SuccessCommandResponse.class,
          FailedCommandResponse.class,
          CommandResponse.class,
          BreakEventBody.class,
          EventNotificationBody.class,
          CommandResponseBody.class,
          BacktraceCommandBody.class,
          FrameObject.class,
          BreakpointBody.class,
          ScopeBody.class,
          ScopeRef.class,
          VersionBody.class,
          AfterCompileBody.class,
          ChangeLiveBody.class,
          ListBreakpointsBody.class,
          ScriptCollectedBody.class,
          FlagsBody.class,
          FlagsBody.FlagInfo.class,

          SomeHandle.class,
          ScriptHandle.class,
          ValueHandle.class,
          RefWithDisplayData.class,
          PropertyObject.class,
          PropertyWithRef.class,
          PropertyWithValue.class,
          ObjectValueHandle.class,
          FunctionValueHandle.class,
          SomeRef.class,
          SomeSerialized.class,
          ContextHandle.class,
          ContextData.class,
          BreakpointInfo.class,
          ScriptWithId.class
          );

      List<DynamicParserImpl> basePackages = Arrays.asList(LiveEditDynamicParser.get());

      parser = new DynamicParserImpl(interfaces, basePackages, false);
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
