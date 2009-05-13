// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.client;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.client.Chrome;
import org.chromium.sdk.client.ChromeTab;
import org.chromium.sdk.client.Version;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler.ListTabsCallback;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler.TabIdAndUrl;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler.VersionCallback;
import org.chromium.sdk.tools.ToolHandler;
import org.chromium.sdk.tools.ToolName;
import org.chromium.sdk.tools.v8.mirror.JsDebugContext;
import org.chromium.sdk.transport.Connection;
import org.chromium.sdk.transport.Message;
import org.chromium.sdk.transport.SocketConnection;
import org.chromium.sdk.transport.Connection.Callback;

/**
 * An implementation of the Chrome interface that uses SocketConnections.
 */
public class ChromeImpl implements Chrome, Callback {
  public static final String DEFAULT_HOSTNAME = "127.0.0.1";
  public static final Version INVALID_VERSION = new Version(0, 0);

  private static final ChromeTab[] EMPTY_TABS = new ChromeTab[0];
  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;
  private String host;
  private int port;
  private int connectionTimeoutMs = DEFAULT_CONNECTION_TIMEOUT_MS;
  private final Map<Integer, ChromeTabImpl> tabUidToChromeTab =
      new HashMap<Integer, ChromeTabImpl>();
  private DevToolsServiceHandler devToolsHandler;
  private Connection connection;

  private Version remoteVersion;

  ChromeImpl(int port) {
    this(DEFAULT_HOSTNAME, port);
  }

  ChromeImpl(String host, int port) {
    this.host = host;
    this.port = port;
  }

  ChromeImpl(Connection connection) {
    this.connection = connection;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ChromeTab[] getTabs() throws IOException {
    ensureConnectionAndService();
    final Semaphore sem = new Semaphore(0);
    final Object[] output = new Object[1];
    devToolsHandler.listTabs(new ListTabsCallback() {
      public void failure(int result) {
        sem.release();
      }
      public void tabsReceived(List<TabIdAndUrl> tabs) {
        output[0] = tabs;
        sem.release();
      }
    });
    try {
      sem.acquire();
    } catch (InterruptedException e) {
      // Fall through
    }

    if (output[0] == null) {
      return EMPTY_TABS;
    }

    List<TabIdAndUrl> entries = (List<TabIdAndUrl>) output[0];
    List<ChromeTab> tabs = new ArrayList<ChromeTab>(entries.size());
    tabUidToChromeTab.clear();
    for (TabIdAndUrl entry : entries) {
      ChromeTabImpl tab = new ChromeTabImpl(entry.id, entry.url, this);
      tabUidToChromeTab.put(entry.id, tab);
      tabs.add(tab);
    }
    return tabs.toArray(new ChromeTab[tabs.size()]);
  }

  /**
   * Creates or gets an existing V8DebuggerToolHandler instance corresponding
   * to the specified {@code tabId}.
   */
  JsDebugContext getDebugContext(int tabUid) {
    ChromeTab tab = tabUidToChromeTab.get(tabUid);
    if (tab == null) {
      return null;
    }
    return tab.getDebugContext();
  }

  @Override
  public void setTimeoutMs(int timeout) {
    this.connectionTimeoutMs = timeout;
  }

  public Connection getConnection() {
    return connection;
  }

  @Override
  public void close(boolean lameduckMode) {
    getConnection().close(lameduckMode);
  }

  @Override
  public void connectionClosed() {
    devToolsHandler.onDebuggerDetached();
    for (Iterator<? extends ChromeTab> it = tabUidToChromeTab.values().iterator(); it.hasNext() ;) {
      JsDebugContext context = it.next().getDebugContext();
      it.remove(); // an attempt to remove it will take place in #sessionTerminated
      context.onDebuggerDetached();
      context.getV8Handler().onDebuggerDetached();
    }
    tabUidToChromeTab.clear();
    connection = null;
  }

  @Override
  public void messageReceived(Message message) {
    ToolName toolName = ToolName.forString(message.getTool());
    if (toolName == null) {
      Logger.getLogger(
          ChromeImpl.class.getName()).log(
              Level.SEVERE,
              MessageFormat.format("Bad 'Tool' header received: {0}", message.getTool()));
      return;
    }
    ToolHandler handler = null;
    switch (toolName) {
      case DEVTOOLS_SERVICE:
        handler = devToolsHandler;
        break;
      case V8_DEBUGGER:
        handler = getDebugContext(Integer.valueOf(message.getDestination())).getV8Handler();
        break;
      default:
        Logger.getLogger(
            ChromeImpl.class.getName()).log(
                Level.SEVERE,
                MessageFormat.format("Unregistered handler for tool: {0}", message.getTool()));
        return;
    }
    handler.handleMessage(message);
  }

  public void sessionTerminated(int tabId) {
    tabUidToChromeTab.remove(tabId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof ChromeImpl)) {
      return false;
    }
    ChromeImpl that = (ChromeImpl) o;
    return this.port == that.port && eq(this.host, that.host);
  }

  @Override
  public int hashCode() {
    return host.hashCode() + port;
  }

  private void ensureConnectionAndService() throws IOException {
    if (connection == null) {
      remoteVersion = null;
      connection = new SocketConnection(host, port, connectionTimeoutMs);
      connection.setCallback(this);
      devToolsHandler = new DevToolsServiceHandler(connection);
    }
    connection.start();
  }

  private static boolean eq(Object left, Object right) {
    return left == right || (left != null && left.equals(right));
  }

  @Override
  public boolean isRemoteVersionCompatible() throws IOException {
    final Version remoteVersion = getRemoteVersion();

    if (remoteVersion == null) {
      return false;
    }
    return SDK_VERSION.isCompatibleWithServer(remoteVersion);
  }

  public Version getRemoteVersion() throws IOException {
    if (remoteVersion == null) {
      ensureConnectionAndService();
      final Semaphore sem = new Semaphore(0);
      final Version[] output = new Version[1];
      devToolsHandler.version(new VersionCallback() {
        public void versionReceived(Version version) {
          output[0] = version;
          sem.release();
        }
      });
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        // Fall through
      }
      remoteVersion = output[0];
      if (remoteVersion == null) {
        remoteVersion = INVALID_VERSION; // mask null version
      }
    }
    return INVALID_VERSION.equals(remoteVersion) // unmask null version
        ? null
        : remoteVersion;
  }
}
