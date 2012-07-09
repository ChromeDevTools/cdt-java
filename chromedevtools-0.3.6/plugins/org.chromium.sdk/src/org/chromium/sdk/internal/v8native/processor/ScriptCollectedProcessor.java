// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.processor;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.DebugSession;
import org.chromium.sdk.internal.v8native.ScriptManager;
import org.chromium.sdk.internal.v8native.protocol.input.EventNotification;
import org.chromium.sdk.internal.v8native.protocol.input.EventNotificationBody;
import org.chromium.sdk.internal.v8native.protocol.input.ScriptCollectedBody;

/**
 * Listens for scripts sent in the "scriptCollected" events and passes their ids to
 * the {@link ScriptManager}.
 */
public class ScriptCollectedProcessor extends V8EventProcessor {

  public ScriptCollectedProcessor(DebugSession debugSession) {
    super(debugSession);
  }

  @Override
  public void messageReceived(EventNotification eventMessage) {
    EventNotificationBody body = eventMessage.body();

    ScriptCollectedBody scriptCollectedBody;
    try {
      scriptCollectedBody = body.asScriptCollectedBody();
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }

    long scriptId = scriptCollectedBody.script().id();

    getDebugSession().getScriptManager().scriptCollected(scriptId);
  }

}
