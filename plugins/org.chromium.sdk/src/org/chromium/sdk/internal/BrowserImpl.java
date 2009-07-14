// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.Version;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.tools.ToolName;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler.TabIdAndUrl;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.Connection.NetListener;

/**
 * A thread-safe implementation of the Browser interface.
 */
public class BrowserImpl implements Browser, NetListener {

  private static final Logger LOGGER = Logger.getLogger(BrowserImpl.class.getName());

  public static final int OPERATION_TIMEOUT_MS = 3000;

  public static final Version INVALID_VERSION = new Version(0, 0);

  private static final BrowserTab[] EMPTY_TABS = new BrowserTab[0];

  /** A mapping of tab IDs to BrowserTabImpls */
  private final Map<Integer, BrowserTabImpl> tabUidToTabImpl =
      Collections.synchronizedMap(new HashMap<Integer, BrowserTabImpl>());

  /** The DevTools service handler for the browser */
  private volatile DevToolsServiceHandler devToolsHandler;

  /** The browser connection (gets opened in the connect() call) */
  private final Connection connection;

  private boolean isNetworkSetUp;

  /**
   * The protocol version supported by this SDK implementation.
   */
  public static final Version PROTOCOL_VERSION = new Version(0, 1);

  BrowserImpl(Connection connection) {
    this.connection = connection;
  }

  public BrowserTab[] getTabs() throws IOException {
    checkConnection();
    removeDetachedTabs();
    List<TabIdAndUrl> entries = devToolsHandler.listTabs(OPERATION_TIMEOUT_MS);
    if (entries.isEmpty()) {
      return EMPTY_TABS;
    }

    List<BrowserTab> browserTabs = new ArrayList<BrowserTab>(entries.size());
    for (TabIdAndUrl entry : entries) {
      BrowserTabImpl tab = tabUidToTabImpl.get(entry.id);
      if (tab == null || !tab.isAttached()) {
        tab = new BrowserTabImpl(entry.id, entry.url, this);
        tabUidToTabImpl.put(entry.id, tab);
      }
      browserTabs.add(tab);
    }
    return browserTabs.toArray(new BrowserTab[browserTabs.size()]);
  }

  private void removeDetachedTabs() {
    for (Iterator<Map.Entry<Integer, BrowserTabImpl>> it = tabUidToTabImpl.entrySet().iterator();
         it.hasNext(); ) {
      Map.Entry<Integer, BrowserTabImpl> entry = it.next();
      if (!entry.getValue().isAttached()) {
        it.remove();
      }
    }
  }

  /**
   * Gets an existing V8DebuggerToolHandler instance corresponding to the
   * specified {@code tabId} (owned by a BrowserTabImpl instance).
   */
  DebugContextImpl getDebugContext(int tabUid) {
    BrowserTabImpl tab = tabUidToTabImpl.get(tabUid);
    if (tab == null) {
      return null;
    }
    return tab.getDebugContext();
  }

  public Connection getConnection() {
    return connection;
  }

  public void disconnect() {
    getConnection().close();
  }

  public void connectionClosed() {
    devToolsHandler.onDebuggerDetached();
    // Use a copy to avoid the underlying map modification in #sessionTerminated
    // invoked through #onDebuggerDetached
    ArrayList<BrowserTabImpl> tabsCopy = new ArrayList<BrowserTabImpl>(tabUidToTabImpl.values());
    for (Iterator<BrowserTabImpl> it = tabsCopy.iterator(); it.hasNext();) {
      it.next().getDebugContext().onDebuggerDetached();
    }
  }

  public void messageReceived(Message message) {
    ToolName toolName = ToolName.forString(message.getTool());
    if (toolName == null) {
      LOGGER.log(Level.SEVERE,
          MessageFormat.format("Bad 'Tool' header received: {0}", message.getTool()));
      return;
    }
    ToolHandler handler = null;
    switch (toolName) {
      case DEVTOOLS_SERVICE:
        handler = devToolsHandler;
        break;
      case V8_DEBUGGER:
        DebugContextImpl debugContext = getDebugContext(Integer.valueOf(message.getDestination()));
        if (debugContext != null) {
          handler = debugContext.getV8Handler();
        }
        break;
      default:
        LOGGER.log(Level.SEVERE,
            MessageFormat.format("Unregistered handler for tool: {0}", message.getTool()));
        return;
    }
    if (handler != null) {
      handler.handleMessage(message);
    } else {
      LOGGER.log(Level.SEVERE,
          MessageFormat.format("null handler for tool: {0}, destination: {1}",
              message.getTool(), message.getDestination()));
    }
  }

  public void sessionTerminated(int tabId) {
    tabUidToTabImpl.remove(tabId);
    if (!hasAttachedTabs() && getConnection().isConnected()) {
      disconnect();
    }
  }

  private boolean hasAttachedTabs() {
    for (BrowserTabImpl tab : tabUidToTabImpl.values()) {
      if (tab.isAttached()) {
        return true;
      }
    }
    return false;
  }

  public void connect() throws UnsupportedVersionException, IOException {
    if (ensureService()) {
      // No need to check the version for an already established connection.
      return;
    }
    Version serverVersion = devToolsHandler.version(OPERATION_TIMEOUT_MS);
    if (serverVersion == null ||
        !BrowserImpl.PROTOCOL_VERSION.isCompatibleWithServer(serverVersion)) {
      isNetworkSetUp = false;
      throw new UnsupportedVersionException(BrowserImpl.PROTOCOL_VERSION, serverVersion);
    }
  }

  private boolean ensureService() throws IOException {
    if (!isNetworkSetUp) {
      devToolsHandler = new DevToolsServiceHandler(connection);
      connection.setNetListener(this);
      this.isNetworkSetUp = true;
    }
    boolean wasConnected = connection.isConnected();
    if (!wasConnected) {
      connection.start();
    }
    return wasConnected;
  }

  // exposed for testing
  /* package private */ DevToolsServiceHandler getDevToolsServiceHandler() {
    return devToolsHandler;
  }

  private void checkConnection() {
    if (connection == null || !connection.isConnected()) {
      throw new IllegalStateException("connection is not started");
    }
  }

}
