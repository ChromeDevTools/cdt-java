// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.ScriptManager;
import org.chromium.sdk.internal.protocol.EventNotification;
import org.chromium.sdk.internal.protocol.EventNotificationBody;
import org.chromium.sdk.internal.protocol.ScriptCollectedBody;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

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
    EventNotificationBody body = eventMessage.getBody();

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
