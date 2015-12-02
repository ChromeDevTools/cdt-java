// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal;

import java.net.SocketAddress;

import org.chromium.sdk.JavascriptVmFactory;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.StandaloneVm;
import org.chromium.sdk.internal.standalonev8.StandaloneVmImpl;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Handshaker;
import org.chromium.sdk.internal.transport.SocketConnection;

/**
 * A default implementation of the BrowserFactory interface.
 * TODO: rename it somehow. It's not only a browser factory.
 */
public class JavascriptVmFactoryImpl extends JavascriptVmFactory {

  public static final JavascriptVmFactoryImpl INSTANCE = new JavascriptVmFactoryImpl();

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

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
