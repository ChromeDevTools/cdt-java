// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import java.util.Arrays;
import java.util.List;

import org.chromium.sdk.internal.liveeditprotocol.LiveEditDynamicParser;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.chromium.sdk.internal.v8native.protocol.input.data.BreakpointInfo;
import org.chromium.sdk.internal.v8native.protocol.input.data.ContextData;
import org.chromium.sdk.internal.v8native.protocol.input.data.ContextHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.FunctionValueHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.ObjectValueHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.PropertyObject;
import org.chromium.sdk.internal.v8native.protocol.input.data.PropertyWithRef;
import org.chromium.sdk.internal.v8native.protocol.input.data.PropertyWithValue;
import org.chromium.sdk.internal.v8native.protocol.input.data.RefWithDisplayData;
import org.chromium.sdk.internal.v8native.protocol.input.data.ScriptHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.ScriptWithId;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeRef;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeSerialized;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;

/**
 * A dynamic implementation of a v8 protocol parser.
 */
public class V8DynamicParser {
  public static DynamicParserImpl<V8NativeProtocolParser> create() {
    try {
      List<Class<?>> interfaces = Arrays.asList(
          IncomingMessage.class,
          EventNotification.class,
          SuccessCommandResponse.class,
          FailedCommandResponse.class,
          FailedCommandResponse.ErrorDetails.class,
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
          ChangeLiveBody.CompileErrorDetails.class,
          ChangeLiveBody.CompileErrorDetails.PositionRange.class,
          ChangeLiveBody.CompileErrorDetails.Position.class,
          RestartFrameBody.class,
          RestartFrameBody.ResultDescription.class,
          ListBreakpointsBody.class,
          ScriptCollectedBody.class,
          FlagsBody.class,
          FlagsBody.FlagInfo.class,
          SetVariableValueBody.class,

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

      List<DynamicParserImpl<?>> basePackages =
          Arrays.<DynamicParserImpl<?>>asList(LiveEditDynamicParser.create());

      return new DynamicParserImpl<V8NativeProtocolParser>(V8NativeProtocolParser.class,
          interfaces, basePackages, false);
    } catch (JsonProtocolModelParseException e) {
      throw new RuntimeException(e);
    }
  }
}
