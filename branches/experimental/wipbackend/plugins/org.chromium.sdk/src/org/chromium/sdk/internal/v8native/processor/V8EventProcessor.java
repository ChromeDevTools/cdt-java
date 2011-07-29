// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.processor;

import org.chromium.sdk.internal.v8native.DebugSession;
import org.chromium.sdk.internal.v8native.protocol.input.EventNotification;

/**
 * An abstract base implementation of DebugContextImpl-aware
 * reply handlers for certain V8 commands.
 * <p>
 * NB! The {@link #messageReceived(org.json.simple.JSONObject)} implementation
 * MUST NOT perform debugger commands in a blocking way the current thread.
 */
public abstract class V8EventProcessor {

  private final DebugSession debugSession;

  public V8EventProcessor(DebugSession debugSession) {
    this.debugSession = debugSession;
  }

  public abstract void messageReceived(EventNotification eventMessage);

  protected DebugSession getDebugSession() {
    return debugSession;
  }
}
