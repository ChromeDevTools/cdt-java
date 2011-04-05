// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

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
import org.chromium.sdk.internal.protocol.data.LiveEditResult;
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
import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;

/**
 * An accessor to dynamic implementation of a v8 protocol parser. Should be replaceable with
 * a similar class that provides access to generated parser implementation.
 */
public class V8ProtocolParserAccess {
  public static JsonProtocolParser get() {
    return parser;
  }

  public static DynamicParserImpl getDynamic() {
    return parser;
  }

  private static final DynamicParserImpl parser;
  static {
    try {
      // TODO(peter.rybin): change to ParserHolder.
      parser = new DynamicParserImpl(new Class<?>[] {
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
          ScriptWithId.class,
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
