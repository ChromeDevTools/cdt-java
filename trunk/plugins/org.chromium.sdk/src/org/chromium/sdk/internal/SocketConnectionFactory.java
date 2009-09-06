// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.io.IOException;
import java.net.SocketAddress;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.internal.transport.Handshaker;
import org.chromium.sdk.internal.transport.SocketConnection;
import org.chromium.sdk.internal.transport.Connection.NetListener;

/**
 * Factory for socket connections. Extremely simple and straight-forward
 * implementation. Note that it works only with stateless {@link Handshaker}s
 * because they are reused for every connection.
 */
public class SocketConnectionFactory implements ConnectionFactory {
  private final SocketAddress endpoint;
  private final int connectionTimeoutMs;
  private final ConnectionLogger.Factory connectionLoggerFactory;
  private final Handshaker handshaker;

  public SocketConnectionFactory(SocketAddress endpoint, int connectionTimeoutMs,
      ConnectionLogger.Factory connectionLoggerFactory, Handshaker handshaker) {
    this.endpoint = endpoint;
    this.connectionTimeoutMs = connectionTimeoutMs;
    this.connectionLoggerFactory = connectionLoggerFactory;
    this.handshaker = handshaker;
  }

  public SocketConnection newOpenConnection(NetListener netListener) throws IOException {
    SocketConnection connection = new SocketConnection(endpoint, connectionTimeoutMs,
        connectionLoggerFactory.newConnectionLogger(), handshaker);
    connection.setNetListener(netListener);
    connection.start();
    return connection;
  }
}
