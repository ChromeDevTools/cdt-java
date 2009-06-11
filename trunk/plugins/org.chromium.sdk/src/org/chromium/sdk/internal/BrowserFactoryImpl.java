// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.SocketConnection;

/**
 * A default, thread-safe implementation of the BrowserFactory interface.
 */
public class BrowserFactoryImpl extends BrowserFactory {

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

  /** A mapping from Endpoints to the BrowserImpl instances. */
  private final Map<Connection, BrowserImpl> endpointToBrowserImpl =
      Collections.synchronizedMap(new HashMap<Connection, BrowserImpl>());

  @Override
  public synchronized Browser create(int port) {
    return cachedBrowserImpl(createConnection(LOCALHOST, port));
  }

  @Override
  public synchronized Browser create(String host, int port) {
    return cachedBrowserImpl(createConnection(host, port));
  }

  public synchronized Browser create(Connection connection) {
    return cachedBrowserImpl(connection);
  }

  protected Connection createConnection(String host, int port) {
    return new SocketConnection(host, port, getTimeout());
  }

  private Browser cachedBrowserImpl(Connection connection) {
    BrowserImpl impl = endpointToBrowserImpl.get(connection);
    if (impl == null) {
      impl = new BrowserImpl(connection);
      endpointToBrowserImpl.put(connection, impl);
    }
    return impl;
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
