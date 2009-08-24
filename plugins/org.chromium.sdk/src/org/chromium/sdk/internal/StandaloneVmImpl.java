// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.StandaloneVm;
import org.chromium.sdk.internal.tools.v8.StandaloneVmSessionManager;
import org.chromium.sdk.internal.transport.Handshaker;
import org.chromium.sdk.internal.transport.SocketConnection;

/**
 * Implementation of {@code StandaloneVm}. Currently knows nothing about
 * contexts, so all existing V8 contexts are presented mixed together.
 */
class StandaloneVmImpl extends JavascriptVmImpl implements StandaloneVm {
  private final StandaloneVmSessionManager sessionManager;

  StandaloneVmImpl(SocketConnection connection, Handshaker.StandaloneV8 handshaker) {
    this.sessionManager = new StandaloneVmSessionManager(this, connection, handshaker);
  }

  @Override
  public DebugSession getDebugSession() {
    return sessionManager.getDebugSession();
  }

  @Override
  public DebugEventListener getDebugEventListener() {
    return sessionManager.getDebugEventListener();
  }

  @Override
  public DebugSessionManager getSessionManager() {
    return sessionManager;
  }

  public boolean attach(DebugEventListener listener) {
    return sessionManager.attach(listener);
  }

  public boolean detach() {
    return sessionManager.detach();
  }

  public boolean isAttached() {
    return sessionManager.isAttached();
  }

  public String getEmbedderName() {
    return sessionManager.getRemoteInfo().getEmbeddingHostName();
  }

  public String getVmVersion() {
    return sessionManager.getRemoteInfo().getV8VmVersion();
  }
}
