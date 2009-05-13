// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.client;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.client.Chrome;
import org.chromium.sdk.client.ChromeFactory;
import org.chromium.sdk.transport.Connection;
import org.chromium.sdk.transport.Connection.Endpoint;

/**
 * A default implementation of the ChromeFactory interface.
 */
public class ChromeFactoryImpl extends ChromeFactory {

  private final Map<Connection.Endpoint, ChromeImpl> endpointToChromeImpl =
      new HashMap<Connection.Endpoint, ChromeImpl>();

  @Override
  public Chrome create(int port) {
    Endpoint endpoint = new Endpoint(ChromeImpl.DEFAULT_HOSTNAME, port);
    ChromeImpl impl = getChromeImpl(endpoint);
    if (impl == null) {
      impl = new ChromeImpl(port);
      putChromeImpl(endpoint, impl);
    }
    return impl;
  }

  @Override
  public Chrome create(String host, int port) {
    Endpoint endpoint = new Endpoint(host, port);
    ChromeImpl impl = getChromeImpl(endpoint);
    if (impl == null) {
      impl = new ChromeImpl(host, port);
      putChromeImpl(endpoint, impl);
    }
    return impl;
  }

  @Override
  public Chrome create(Connection connection) {
    Endpoint endpoint = connection.getEndpoint();
    ChromeImpl impl = getChromeImpl(endpoint);
    if (impl == null) {
      impl = new ChromeImpl(connection);
      putChromeImpl(endpoint, impl);
    }
    return impl;
  }

  private void putChromeImpl(Endpoint endpoint, ChromeImpl impl) {
    endpointToChromeImpl.put(endpoint, impl);
  }

  private ChromeImpl getChromeImpl(Endpoint endpoint) {
    return endpointToChromeImpl.get(endpoint);
  }

}
