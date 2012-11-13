// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

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

  public void runInDispatchThread(Runnable callback) {
    callback.run();
  }

  public boolean isConnected() {
    return isRunning;
  }

  public void close() {
    boolean sendEos = isRunning;
    isRunning = false;
    if (netListener != null) {
      if (sendEos) {
        netListener.eosReceived();
      }
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

  public static final Handshaker.StandaloneV8 HANDSHAKER = new Handshaker.StandaloneV8() {
    @Override
    public void perform(LineReader input, OutputStream output) throws IOException {
    }

    @Override
    public Future<RemoteInfo> getRemoteInfo() {
      return remoteInfoFuture;
    }

    private final FutureTask<RemoteInfo> remoteInfoFuture;
    {
      remoteInfoFuture = new FutureTask<RemoteInfo>(new Callable<RemoteInfo>() {
        @Override
        public RemoteInfo call() throws Exception {
          return new RemoteInfo() {
            @Override public String getProtocolVersion() {
              return "1";
            }
            @Override public String getV8VmVersion() {
              return "3.12.1 test fixture";
            }
            @Override public String getEmbeddingHostName() {
              return "junit test fixture";
            }
          };
        }
      });
      remoteInfoFuture.run();
    }
  };

}
