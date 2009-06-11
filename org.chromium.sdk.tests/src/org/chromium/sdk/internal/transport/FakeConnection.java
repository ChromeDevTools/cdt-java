// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Message;

/**
 * A fake Connection that allows specifying a message responder (aka ChromeStub).
 */
public class FakeConnection implements Connection {

  private boolean isRunning;
  private final ChromeStub responder;
  private NetListener netListener;

  public FakeConnection(ChromeStub responder) {
    this.responder = responder;
  }

  @Override
  public void send(Message message) {
    assertTrue(isRunning);
    assertNotNull(responder);
    Message response = responder.respondTo(message);
    if (response != null) {
      netListener.messageReceived(response);
    }
  }

  @Override
  public boolean isConnected() {
    return isRunning;
  }

  @Override
  public void close() {
    isRunning = false;
  }

  @Override
  public void setNetListener(NetListener netListener) {
    this.netListener = netListener;
    responder.setNetListener(netListener);
  }

  @Override
  public void start() throws IOException {
    isRunning = true;
  }

}
