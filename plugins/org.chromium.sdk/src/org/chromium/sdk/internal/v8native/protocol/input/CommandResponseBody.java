// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.v8native.protocol.input.data.ScriptHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.json.simple.JSONObject;

/**
 * This is empty base type for all command response body types. The actual type
 * depends on a particular command. Note that in JSON sometimes it is an array rather than object
 * (for scripts).
 */
@JsonType(subtypesChosenManually=true)
public interface CommandResponseBody {
  @JsonSubtypeCasting
  BacktraceCommandBody asBacktraceCommandBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  List<ScriptHandle> asScripts() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  BreakpointBody asBreakpointBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  // map refId -> ValueHandle
  JSONObject asLookupMap() throws JsonProtocolParseException;

  @JsonSubtypeCasting(reinterpret=true)
  ValueHandle asEvaluateBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  ScopeBody asScopeBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  VersionBody asVersionBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  ChangeLiveBody asChangeLiveBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  RestartFrameBody asRestartFrameBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  ListBreakpointsBody asListBreakpointsBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  FlagsBody asFlagsBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  SetVariableValueBody asSetVariableValueBody() throws JsonProtocolParseException;
}
