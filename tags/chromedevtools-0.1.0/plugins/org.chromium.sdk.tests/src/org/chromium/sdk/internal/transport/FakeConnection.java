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

  public void send(Message message) {
    assertTrue(isRunning);
    assertNotNull(responder);
    Message response = responder.respondTo(message);
    if (response != null) {
      netListener.messageReceived(response);
    }
  }

  public boolean isConnected() {
    return isRunning;
  }

  public void close() {
    isRunning = false;
    if (netListener != null) {
      netListener.connectionClosed();
    }
  }

  public void setNetListener(NetListener netListener) {
    this.netListener = netListener;
    responder.setNetListener(netListener);
  }

  public void start() throws IOException {
    isRunning = true;
  }

}
