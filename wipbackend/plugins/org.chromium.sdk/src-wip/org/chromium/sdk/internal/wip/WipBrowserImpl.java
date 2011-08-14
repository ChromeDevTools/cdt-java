// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.chromium.sdk.Browser;
import org.chromium.sdk.wip.WipBackend;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserFactory;

/**
 * Implements {@link Browser} API that offers connection to a browser tab
 * via WebInspector 'WIP' Protocol.
 */
public class WipBrowserImpl implements WipBrowser {
  private final InetSocketAddress socketAddress;
  private final WipBrowserFactory.LoggerFactory connectionLoggerFactory;

  public WipBrowserImpl(InetSocketAddress socketAddress,
      WipBrowserFactory.LoggerFactory connectionLoggerFactory) {
    this.socketAddress = socketAddress;
    this.connectionLoggerFactory = connectionLoggerFactory;
  }

  @Override
  public List<? extends WipTabConnector> getTabs(WipBackend wipBackend) throws IOException {
    WipBackendBase backendBase = WipBackendBase.castArgument(wipBackend);
    return backendBase.getTabs(this);
  }

  public InetSocketAddress getSocketAddress() {
    return socketAddress;
  }

  public WipBrowserFactory.LoggerFactory getConnectionLoggerFactory() {
    return connectionLoggerFactory;
  }

  /**
   * A convenience method for any currently unsupported operation. It nicely co-works with
   * a return statements.
   */
  public static <T> T throwUnsupported() {
    throw new UnsupportedOperationException();
  }
}
