// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserFactory;

/**
 * Implements {@link Browser} API that offers connection to a browser tab
 * via WebInspector 'WIP' Protocol.
 * TODO: make class actually implement {@link Browser}.
 */
public class WipBrowserImpl implements WipBrowser {
  private final InetSocketAddress socketAddress;
  private final WipBrowserFactory.LoggerFactory connectionLoggerFactory;

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

  public WipBrowserImpl(InetSocketAddress socketAddress,
      WipBrowserFactory.LoggerFactory connectionLoggerFactory) {
    this.socketAddress = socketAddress;
    this.connectionLoggerFactory = connectionLoggerFactory;
  }

  @Override
  public Browser.TabConnector getTabConnector(int tabId) {
    return new TabConnectorImpl(socketAddress, "/devtools/page/" + tabId);
  }

  private class TabConnectorImpl implements Browser.TabConnector {
    private final InetSocketAddress connectorSocketAddress;
    private final String resourceId;

    TabConnectorImpl(InetSocketAddress socketAddress, String resourceId) {
      this.connectorSocketAddress = socketAddress;
      this.resourceId = resourceId;
    }

    @Override
    public String getUrl() {
      return "<unknown url>";
    }

    @Override
    public boolean isAlreadyAttached() {
      return false;
    }

    @Override
    public BrowserTab attach(TabDebugEventListener listener) throws IOException {
      ConnectionLogger connectionLogger = connectionLoggerFactory.newTabConnectionLogger();
      WsConnection socket = WsConnection.connect(connectorSocketAddress,
          DEFAULT_CONNECTION_TIMEOUT_MS, resourceId, "empty origin", connectionLogger);

      return new WipTabImpl(socket, WipBrowserImpl.this, listener);
    }
  }

  /**
   * A convenience method for any currently unsupported operation. It nicely co-works with
   * a return statements.
   */
  public static <T> T throwUnsupported() {
    throw new UnsupportedOperationException();
  }
}
