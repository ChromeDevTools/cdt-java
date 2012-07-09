// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.input;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;

/**
 * This is empty base type for all event notification body types. The actual type
 * depends on a particular event.
 */
@JsonType(subtypesChosenManually=true)
public interface EventNotificationBody {
  @JsonSubtypeCasting
  BreakEventBody asBreakEventBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  AfterCompileBody asAfterCompileBody() throws JsonProtocolParseException;

  @JsonSubtypeCasting
  ScriptCollectedBody asScriptCollectedBody() throws JsonProtocolParseException;
}
