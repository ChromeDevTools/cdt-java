// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Handshaker;
import org.chromium.sdk.internal.transport.SocketConnection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A default, thread-safe implementation of the BrowserFactory interface.
 */
public class BrowserFactoryImpl extends BrowserFactory {

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

  /** A mapping from Endpoints to the BrowserImpl instances. */
  private final Map<Connection, BrowserImpl> endpointToBrowserImpl =
      Collections.synchronizedMap(new HashMap<Connection, BrowserImpl>());

  @Override
  public synchronized Browser create(int port, ConnectionLogger connectionLogger) {
    Connection connection = createConnection(LOCALHOST, port, connectionLogger);
    return createBrowser(connection, connectionLogger != null);
  }

  @Override
  public synchronized Browser create(String host, int port, ConnectionLogger connectionLogger) {
    Connection connection = createConnection(host, port, connectionLogger);
    return createBrowser(connection, connectionLogger != null);
  }

  // Debug entry (no logger by definition)
  public synchronized Browser create(Connection connection) {
    return createBrowser(connection, false);
  }

  public synchronized Browser createBrowser(Connection connection, boolean hasLogger) {
    if (hasLogger) {
      return getNewBrowserImpl(connection);
    } else {
      return getCachedBrowserImpl(connection);
    }
  }

  protected Connection createConnection(String host, int port, ConnectionLogger connectionLogger) {
    return new SocketConnection(host, port, getTimeout(), connectionLogger, Handshaker.CHROMIUM);
  }

  private BrowserImpl getCachedBrowserImpl(Connection connection) {
    // TODO(prybin): maybe separate connection (object) from endpoint (map key).
    BrowserImpl impl = endpointToBrowserImpl.get(connection);
    if (impl == null) {
      impl = new BrowserImpl(connection);
      endpointToBrowserImpl.put(connection, impl);
    }
    return impl;
  }

  private BrowserImpl getNewBrowserImpl(Connection connection) {
    return new BrowserImpl(connection);
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
