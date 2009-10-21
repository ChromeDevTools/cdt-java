// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.Version;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.tools.ToolName;
import org.chromium.sdk.internal.tools.ToolOutput;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler.TabIdAndUrl;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.Connection.NetListener;

/**
 * A thread-safe implementation of the Browser interface.
 */
public class BrowserImpl implements Browser {

  private static final Logger LOGGER = Logger.getLogger(BrowserImpl.class.getName());

  public static final int OPERATION_TIMEOUT_MS = 3000;

  /**
   * The protocol version supported by this SDK implementation.
   */
  public static final Version PROTOCOL_VERSION = new Version(0, 1);

  private final ConnectionSessionManager sessionManager = new ConnectionSessionManager();

  /** The browser connection (gets opened in session). */
  private final ConnectionFactory connectionFactory;

  BrowserImpl(ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  public TabFetcher createTabFetcher() throws IOException, UnsupportedVersionException {
    SessionManager.Ticket<Session> ticket = connectInternal();
    return new TabFetcherImpl(ticket);
  }

  private SessionManager.Ticket<Session> connectInternal() throws IOException,
      UnsupportedVersionException {
    try {
      return sessionManager.connect();
    } catch (ExceptionWrapper eWrapper) {
      eWrapper.rethrow();
      // Not reachable.
      throw new RuntimeException();
    }
  }

  /**
   * Object that lives during one connection period. Browser should be able to
   * reconnect (because we want to support attach-detach-attach sequence). On
   * reconnect new session should be created. Each browser tab should be linked
   * to a particular session.
   */
  public class Session extends SessionManager.SessionBase<Session> {

    private final AtomicBoolean alreadyClosingSession = new AtomicBoolean(false);

    private final CloseableMap<Integer, ToolHandler> tabId2ToolHandler = CloseableMap.newMap();

    // TODO(peter.rybin): get rid of this structure (if we can get rid
    // of corresponding notification)
    private final Map<Integer, DebugSession> tabId2DebugSession =
        new ConcurrentHashMap<Integer, DebugSession>();

    /** The DevTools service handler for the browser. */
    private volatile DevToolsServiceHandler devToolsHandler;

    /** Open connection which is used by the session. */
    private final Connection sessionConnection;

    Session() throws IOException, UnsupportedVersionException {
      super(sessionManager);

      devToolsHandler = new DevToolsServiceHandler(devToolsToolOutput);

      sessionConnection = connectionFactory.newOpenConnection(netListener);

      String serverVersionString;
      try {
        serverVersionString = devToolsHandler.version(OPERATION_TIMEOUT_MS);
      } catch (TimeoutException e) {
        throw new IOException("Failed to get protocol version from remote", e);
      }
      if (serverVersionString == null) {
        throw new UnsupportedVersionException(BrowserImpl.PROTOCOL_VERSION, null);
      }
      Version serverVersion = Version.parseString(serverVersionString);
      if (serverVersion == null ||
          serverVersion.compareTo(BrowserImpl.PROTOCOL_VERSION) < 0) {
        throw new UnsupportedVersionException(BrowserImpl.PROTOCOL_VERSION, serverVersion);
      }
    }

    @Override
    protected Session getThisAsSession() {
      return this;
    }

    @Override
    protected void lastTicketDismissed() {
      boolean res = alreadyClosingSession.compareAndSet(false, true);
      if (!res) {
        // already closing
        return;
      }
      closeSession();
      sessionConnection.close();
    }

    void registerTab(int destinationTabId, ToolHandler toolHandler, DebugSession debugSession)
        throws IOException {
      try {
        tabId2ToolHandler.put(destinationTabId, toolHandler);
      } catch (IllegalStateException e) {
        throw new IOException("Tab id=" + destinationTabId + " cannot be attached");
      }
      tabId2DebugSession.put(destinationTabId, debugSession);
    }

    void unregisterTab(int destinationTabId) {
      tabId2DebugSession.remove(destinationTabId);
      tabId2ToolHandler.remove(destinationTabId);
    }

    private DevToolsServiceHandler getDevToolsServiceHandler() {
      return devToolsHandler;
    }

    @Override
    protected void checkHealth() {
      // We do not actually interrupt here. It's more an assert for now: we throw an exception,
      // if connection is unexpectedly closed.
      if (sessionConnection.isConnected()) {
        // All OK
        return;
      }
      // We should not be here
      LOGGER.severe("checkHealth in BrowserImpl found a consistnecy problem; " +
          "current session is broken and therefore terminated");
      interruptSession();
      closeSession();
    }

    private void checkConnection() {
      if (!sessionConnection.isConnected()) {
        throw new IllegalStateException("connection is not started");
      }
    }

    private final NetListener netListener = new NetListener() {
      public void connectionClosed() {
        devToolsHandler.onDebuggerDetached();
        // Use a copy to avoid the underlying map modification in #sessionTerminated
        // invoked through #onDebuggerDetached
        Collection<DebugSession> copy = new ArrayList<DebugSession>(tabId2DebugSession.values());
        for (DebugSession session : copy) {
          session.onDebuggerDetached();
        }
      }

      public void messageReceived(Message message) {
        ToolName toolName = ToolName.forString(message.getTool());
        if (toolName == null) {
          LOGGER.log(Level.SEVERE, "Bad 'Tool' header received: {0}", message.getTool());
          return;
        }
        ToolHandler handler = null;
        switch (toolName) {
          case DEVTOOLS_SERVICE:
            handler = devToolsHandler;
            break;
          case V8_DEBUGGER:
            handler = tabId2ToolHandler.get(Integer.valueOf(message.getDestination()));
            break;
          default:
            LOGGER.log(Level.SEVERE, "Unregistered handler for tool: {0}", message.getTool());
            return;
        }
        if (handler != null) {
          handler.handleMessage(message);
        } else {
          LOGGER.log(
              Level.SEVERE,
              "null handler for tool: {0}, destination: {1}",
              new Object[] {message.getTool(), message.getDestination()});
        }
      }
      public void eosReceived() {
        boolean res = alreadyClosingSession.compareAndSet(false, true);
        if (!res) {
          // already closing
          return;
        }

        Collection<ToolHandler> allHandlers = tabId2ToolHandler.close().values();
        for (ToolHandler handler : allHandlers) {
          handler.handleEos();
        }

        devToolsHandler.handleEos();
        Collection<? extends RuntimeException> problems = interruptSession();
        for (RuntimeException ex : problems) {
          LOGGER.log(Level.SEVERE, "Failure in closing connections", ex);
        }
        closeSession();
      }
    };

    private final ToolOutput devToolsToolOutput = new ToolOutput() {
      public void send(String content) {
        Message message =
            MessageFactory.createMessage(ToolName.DEVTOOLS_SERVICE.value, null, content);
        sessionConnection.send(message);
      }
    };

    public BrowserImpl getBrowser() {
      return BrowserImpl.this;
    }
  }

  private class TabFetcherImpl implements TabFetcher {
    private final SessionManager.Ticket<Session> ticket;

    TabFetcherImpl(SessionManager.Ticket<Session> ticket) {
      this.ticket = ticket;
    }

    public List<? extends TabConnector> getTabs() {
      Session session = ticket.getSession();
      session.checkConnection();
      List<TabIdAndUrl> entries = session.devToolsHandler.listTabs(OPERATION_TIMEOUT_MS);
      List<TabConnectorImpl> tabConnectors = new ArrayList<TabConnectorImpl>(entries.size());
      for (TabIdAndUrl entry : entries) {
        tabConnectors.add(new TabConnectorImpl(entry.id, entry.url, ticket));
      }
      return tabConnectors;
    }

    public void dismiss() {
      ticket.dismiss();
    }
  }

  private class TabConnectorImpl implements TabConnector {
    private final int tabId;
    private final String url;
    // Ticket that we inherit from TabFetcher.
    private final SessionManager.Ticket<Session> ticket;

    TabConnectorImpl(int tabId, String url, SessionManager.Ticket<Session> ticket) {
      this.tabId = tabId;
      this.url = url;
      this.ticket = ticket;
    }

    public String getUrl() {
      return url;
    }

    public boolean isAlreadyAttached() {
      return ticket.getSession().tabId2ToolHandler.get(tabId) != null;
    }

    public BrowserTab attach(TabDebugEventListener listener) throws IOException {
      SessionManager.Ticket<Session> ticket;
      try {
        ticket = connectInternal();
      } catch (UnsupportedVersionException e) {
        // This exception should have happened on tab fetcher creation.
        throw new IOException("Unexpected version problem", e);
      }

      Session session = ticket.getSession();

      BrowserTabImpl browserTab = null;
      try {
        browserTab = new BrowserTabImpl(tabId, url, session.sessionConnection, ticket);
      } finally {
        if (browserTab == null) {
          ticket.dismiss();
        }
      }
      // From now on browserTab is responsible for the ticket.
      browserTab.attach(listener);
      return browserTab;
    }
  }

  /**
   * With this session manager we expect all ticket owners to call dismiss in any
   * circumstances.
   */
  private class ConnectionSessionManager extends
      SessionManager<BrowserImpl.Session, ExceptionWrapper>  {
    @Override
    protected Session newSessionObject() throws ExceptionWrapper {
      try {
        return new Session();
      } catch (IOException e) {
        throw ExceptionWrapper.wrap(e);
      } catch (UnsupportedVersionException e) {
        throw ExceptionWrapper.wrap(e);
      }
    }
  }

  private static abstract class ExceptionWrapper extends Exception {
    abstract void rethrow() throws IOException, UnsupportedVersionException;

    static ExceptionWrapper wrap(final IOException e) {
      return new ExceptionWrapper() {
        @Override
        void rethrow() throws IOException {
          throw e;
        }
      };
    }

    static ExceptionWrapper wrap(final UnsupportedVersionException e) {
      return new ExceptionWrapper() {
        @Override
        void rethrow() throws UnsupportedVersionException {
          throw e;
        }
      };
    }
  }

  public boolean isTabConnectedForTest(int tabId) {
    Session session = sessionManager.getCurrentSessionForTest();
    if (session == null) {
      return false;
    }
    return session.tabId2ToolHandler.get(tabId) != null;
  }

  public DevToolsServiceHandler getDevToolsServiceHandlerForTests() {
    return sessionManager.getCurrentSessionForTest().getDevToolsServiceHandler();
  }

  public boolean isConnectedForTests() {
    return sessionManager.getCurrentSessionForTest() != null;
  }
}
