// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.shellprotocol;

import java.io.IOException;

import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Connection.NetListener;

/**
 * Factory that can be used when several connections to the same
 * endpoint are needed. {@link Connection} does not support reconnection, and
 * this factory can be used instead.
 */
public interface ConnectionFactory {
  /**
   * Creates new connection and starts it. Does not check whether previous connection
   * has already finished.
   * @return already started connection with netListener set
   */
  Connection newOpenConnection(NetListener netListener) throws IOException;
}
