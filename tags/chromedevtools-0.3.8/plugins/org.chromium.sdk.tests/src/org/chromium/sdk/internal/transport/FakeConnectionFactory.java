// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.IOException;

import org.chromium.sdk.internal.shellprotocol.ConnectionFactory;
import org.chromium.sdk.internal.transport.Connection.NetListener;

/**
 * Provides a limited implementation of {@link #newOpenConnection(NetListener)}:
 * it can be used for creating only one connection. Subsequent calls throw exception.
 * This should be enough for tests.
 */
public class FakeConnectionFactory implements ConnectionFactory {
  private final Connection connection;
  private boolean alreadyCreated = false;

  public FakeConnectionFactory(ChromeStub responder) {
    this(new FakeConnection(responder));
  }

  public FakeConnectionFactory(Connection connection) {
    this.connection = connection;
  }

  public Connection newOpenConnection(NetListener netListener) throws IOException {
    if (alreadyCreated) {
      throw new IllegalStateException();
    }
    alreadyCreated = true;
    connection.setNetListener(netListener);
    connection.start();
    return connection;
  }
}
