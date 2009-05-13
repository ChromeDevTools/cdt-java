// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.chromium.debug.core.tools.ToolHandler;
import org.chromium.debug.core.tools.ToolName;
import org.chromium.debug.core.tools.v8.request.V8DebugRequestMessage;
import org.chromium.debug.core.transport.SocketAgent.SocketAgentListener;
import org.chromium.debug.core.util.JsonUtil;
import org.chromium.debug.core.util.LoggingUtil;
import org.eclipse.osgi.util.NLS;

/**
 * Dispatches network events originating from the SocketAgent.
 */
public class SocketConnection extends AbstractConnection implements SocketAgentListener {

  private final SocketAgent connector;

  public SocketConnection(String host, int port) {
    this.connector = new SocketAgent(new InetSocketAddress(host, port), this);
  }

  public void sendToDebugger(String tabUid, V8DebugRequestMessage message)
      throws IOException {
    send(MessageFactory.getInstance().debuggerCommand(
        tabUid, JsonUtil.streamAwareToJson(message)));
  }

  public boolean isConnected() {
    return connector.isAttached();
  }

  @Override
  public void send(Message message) {
    checkAttached();
    connector.sendMessage(message);
  }

  @Override
  public synchronized void startup() throws IOException {
    try {
      if (!connector.isAttached()) {
        connector.attach();
      }
    } catch (IOException e) {
      socketClosed();
      throw e;
    }
  }

  @Override
  public synchronized void shutdown(boolean lameduckMode) {
    if (connector.isAttached()) {
      connector.detach(lameduckMode);
    }
  }

  @Override
  public Exception getTerminatedException() {
    return connector.getTerminatedException();
  }

  @Override
  public void messageReceived(Message message) {
    ToolName tool = ToolName.forString(message.getTool());
    if (tool == null) {
      LoggingUtil.logTransport(
          NLS.bind("Unknown Tool header value: {0}", message.getTool())); //$NON-NLS-1$
      return;
    }
    ToolHandler toolHandler = toolToHandlerMap.get(tool);
    if (toolHandler == null) {
      LoggingUtil.logTransport(
          NLS.bind("No tool handler for tool: {0}", message.getTool())); //$NON-NLS-1$
      return;
    }
    try {
      toolHandler.handleMessage(message);
    } catch (Exception e) {
      LoggingUtil.log(SocketConnection.class.getName(), Level.SEVERE,
          NLS.bind("Exception in {0}", toolHandler), e); //$NON-NLS-1$
    }
  }

  @Override
  public void socketClosed() {
    for (ToolHandler tool : toolToHandlerMap.values()) {
      tool.onDebuggerDetached();
    }
    toolToHandlerMap.clear();
  }

  private void checkAttached() {
    if (!isConnected()) {
      throw new IllegalStateException("Connector not attached"); //$NON-NLS-1$
    }
  }
}
