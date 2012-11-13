// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.net.SocketAddress;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.StandaloneVm;
import org.chromium.sdk.internal.shellprotocol.BrowserImpl;
import org.chromium.sdk.internal.shellprotocol.ConnectionFactory;
import org.chromium.sdk.internal.shellprotocol.SocketConnectionFactory;
import org.chromium.sdk.internal.standalonev8.StandaloneVmImpl;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Handshaker;
import org.chromium.sdk.internal.transport.SocketConnection;

/**
 * A default implementation of the BrowserFactory interface.
 * TODO: rename it somehow. It's not only a browser factory.
 */
public class BrowserFactoryImpl extends BrowserFactory {

  public static final BrowserFactoryImpl INSTANCE = new BrowserFactoryImpl();

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

  @Override
  public Browser create(SocketAddress socketAddress,
      ConnectionLogger.Factory connectionLoggerFactory) {
    SocketConnectionFactory socketConnectionFactory = new SocketConnectionFactory(socketAddress,
        getTimeout(), connectionLoggerFactory, Handshaker.CHROMIUM);
    return new BrowserImpl(socketConnectionFactory);
  }

  // Debug entry (no logger by definition)
  Browser create(ConnectionFactory connectionFactory) {
    return new BrowserImpl(connectionFactory);
  }

  @Override
  public StandaloneVm createStandalone(SocketAddress socketAddress,
      ConnectionLogger connectionLogger) {
    Handshaker.StandaloneV8 handshaker = new Handshaker.StandaloneV8Impl();
    SocketConnection connection =
        new SocketConnection(socketAddress, getTimeout(), connectionLogger, handshaker);
    return createStandalone(connection, handshaker);
  }

  // Debug entry (no logger by definition)
  StandaloneVmImpl createStandalone(Connection connection, Handshaker.StandaloneV8 handshaker) {
    return new StandaloneVmImpl(connection, handshaker);
  }

  private int getTimeout() {
    String timeoutString = System.getProperty(
        "org.chromium.sdk.client.connection.timeoutMs",
        String.valueOf(DEFAULT_CONNECTION_TIMEOUT_MS));
    int timeoutMs = DEFAULT_CONNECTION_TIMEOUT_MS;
    try {
      timeoutMs = Integer.parseInt(timeoutString);
    } catch (NumberFormatException e) {
      // fall through and use the default value
    }
    return timeoutMs;
  }

}
