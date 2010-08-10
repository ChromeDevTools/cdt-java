// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.io.IOException;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.tools.ToolName;
import org.chromium.sdk.internal.tools.ToolOutput;
import org.chromium.sdk.internal.tools.v8.ChromeDevToolSessionManager;
import org.chromium.sdk.internal.tools.v8.ChromeDevToolSessionManager.AttachmentFailureException;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Message;

/**
 * A default, thread-safe implementation of the BrowserTab interface.
 */
public class BrowserTabImpl extends JavascriptVmImpl implements BrowserTab {

  /** Tab ID as reported by the DevTools server. */
  private final int tabId;

  /** The primary tab URL. */
  private final String url;

  private final SessionManager.Ticket<BrowserImpl.Session> connectionTicket;

  private final ChromeDevToolSessionManager devToolSessionManager;

  /** The listener to report debug events to. */
  private DebugEventListener debugEventListener = null;

  /** The listener to report browser-related debug events to. */
  private TabDebugEventListener tabDebugEventListener = null;

  public BrowserTabImpl(int tabId, String url, Connection connection,
      SessionManager.Ticket<BrowserImpl.Session> ticket) throws IOException {
    this.tabId = tabId;
    this.url = url;
    this.connectionTicket = ticket;
    String tabIdString = String.valueOf(tabId);
    ChromeDevToolOutput chromeDevToolOutput = new ChromeDevToolOutput(tabIdString, connection);
    this.devToolSessionManager = new ChromeDevToolSessionManager(this, chromeDevToolOutput);

    ToolHandler toolHandler = devToolSessionManager.getToolHandler();
    // After this statement we are responsible for dismissing our ticket (we do it via eos message).
    getBrowserConnectionSession().registerTab(tabId, toolHandler,
        this.devToolSessionManager.getDebugSession());
  }

  public String getUrl() {
    return url;
  }

  public int getId() {
    return tabId;
  }

  @Override
  public DebugSession getDebugSession() {
    return devToolSessionManager.getDebugSession();
  }

  public synchronized TabDebugEventListener getTabDebugEventListener() {
    return tabDebugEventListener;
  }

  public Browser getBrowser() {
    return getBrowserConnectionSession().getBrowser();
  }

  public BrowserImpl.Session getBrowserConnectionSession() {
    return connectionTicket.getSession();
  }

  synchronized void attach(TabDebugEventListener listener) throws IOException {
    this.tabDebugEventListener = listener;
    this.debugEventListener = listener.getDebugEventListener();

    boolean normalExit = false;
    try {
      Result result;
      try {
        result = devToolSessionManager.attachToTab();
      } catch (AttachmentFailureException e) {
        throw newIOException(null, e);
      }
      if (Result.OK != result) {
        throw new IOException("Failed to attach with result: " + result);
      }
      normalExit = true;
    } finally {
      if (!normalExit) {
        devToolSessionManager.cutTheLineMyself();
      }
    }
  }

  public boolean detach() {
    Result result = devToolSessionManager.detachFromTab();
    return Result.OK == result;
  }

  public boolean isAttached() {
    return devToolSessionManager.isAttachedForUi();
  }

  public void sessionTerminated() {
    //browserSession.sessionTerminated(this.tabId);
  }

  public ToolHandler getV8ToolHandler() {
    return devToolSessionManager.getToolHandler();
  }

  public ChromeDevToolSessionManager getSessionManager() {
    return devToolSessionManager;
  }

  public void handleEosFromToolService() {
    getBrowserConnectionSession().unregisterTab(tabId);
    connectionTicket.dismiss();
  }

  private static class ChromeDevToolOutput implements ToolOutput {
    private final String destination;
    private final Connection connection;

    ChromeDevToolOutput(String destination, Connection connection) {
      this.destination = destination;
      this.connection = connection;
    }


    public void send(String content) {
      Message message =
          MessageFactory.createMessage(ToolName.V8_DEBUGGER.value, destination, content);
      connection.send(message);
    }
  }

  public DebugEventListener getDebugEventListener() {
    return debugEventListener;
  }
}
