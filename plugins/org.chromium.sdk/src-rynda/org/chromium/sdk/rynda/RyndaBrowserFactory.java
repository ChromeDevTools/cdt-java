// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.rynda;

import java.net.InetSocketAddress;

import org.chromium.sdk.Browser;
import org.chromium.sdk.ConnectionLogger;

/**
 * A factory for connections via WebInspector (Rynda) protocol.
 */
public interface RyndaBrowserFactory {

  RyndaBrowserFactory INSTANCE = new org.chromium.sdk.internal.rynda.RyndaBrowserFactoryImpl();

  /**
   * TODO(peter.rybin): This should return a regular {@link Browser} when we have tab access
   * in protocol.
   */
  RyndaBrowser createRyndaConnected(InetSocketAddress socketAddress,
      LoggerFactory connectionLoggerFactory);

  interface LoggerFactory {
    ConnectionLogger newBrowserConnectionLogger();

    ConnectionLogger newTabConnectionLogger();
  }
}
