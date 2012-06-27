// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.AbstractList;
import java.util.List;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.transport.SocketWrapper;
import org.chromium.sdk.internal.transport.SocketWrapper.LoggableInputStream;
import org.chromium.sdk.internal.transport.SocketWrapper.LoggableOutputStream;
import org.chromium.sdk.internal.websocket.HandshakeUtil;
import org.chromium.sdk.internal.websocket.Hybi00WsConnection;
import org.chromium.sdk.internal.websocket.Hybi17WsConnection;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.wip.protocol.WipParserAccess;
import org.chromium.sdk.internal.wip.protocol.input.WipTabList;
import org.chromium.sdk.internal.wip.protocol.input.WipTabList.TabDescription;
import org.chromium.sdk.wip.WipBrowser.WipTabConnector;
import org.chromium.sdk.wip.WipBrowserFactory.LoggerFactory;
import org.chromium.sdk.wip.WipBrowserTab;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WipBackendImpl extends WipBackendBase {
  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

  private static final boolean USE_OLD_WEBSOCKET = false;

  private static final String ID = "WK@118685";
  private static final String DESCRIPTION =
      "Google Chrome/Chromium: 21.0.1180.*\n" +
      "WebKit revision: 118685\n";

  public WipBackendImpl() {
    super(ID, DESCRIPTION);
  }

  @Override
  public List<? extends WipTabConnector> getTabs(final WipBrowserImpl browserImpl)
      throws IOException {
    InetSocketAddress socketAddress = browserImpl.getSocketAddress();

    String content = readHttpResponseContent(socketAddress, "/json",
        browserImpl.getConnectionLoggerFactory());

    final List<WipTabList.TabDescription> list = parseJsonReponse(content);

    return new AbstractList<WipTabConnector>() {
      @Override
      public WipTabConnector get(int index) {
        return new TabConnectorImpl(list.get(index), browserImpl);
      }

      @Override
      public int size() {
        return list.size();
      }
    };
  }

  private class TabConnectorImpl implements WipTabConnector {
    private final TabDescription description;
    private final WipBrowserImpl browserImpl;

    private TabConnectorImpl(TabDescription description, WipBrowserImpl browserImpl) {
      this.description = description;
      this.browserImpl = browserImpl;
    }

    @Override
    public boolean isAlreadyAttached() {
      return description.webSocketDebuggerUrl() == null;
    }

    @Override
    public String getUrl() {
      return description.url();
    }

    @Override
    public String getTitle() {
      return description.title();
    }

    @Override
    public WipBrowserTab attach(TabDebugEventListener listener) throws IOException {
      LoggerFactory connectionLoggerFactory = browserImpl.getConnectionLoggerFactory();
      ConnectionLogger connectionLogger;
      if (connectionLoggerFactory == null) {
        connectionLogger = null;
      } else {
        connectionLogger = connectionLoggerFactory.newTabConnectionLogger();
      }
      String webSocketDebuggerUrl = description.webSocketDebuggerUrl();

      if (webSocketDebuggerUrl == null) {
        throw new IOException("Tab is already attached");
      }

      URI uri = URI.create(webSocketDebuggerUrl);
      WsConnection socket;
      if (USE_OLD_WEBSOCKET) {
        socket = Hybi00WsConnection.connect(browserImpl.getSocketAddress(),
            DEFAULT_CONNECTION_TIMEOUT_MS, uri.getPath(), "empty origin", connectionLogger);
      } else {
        socket = Hybi17WsConnection.connect(browserImpl.getSocketAddress(),
            DEFAULT_CONNECTION_TIMEOUT_MS, uri.getPath(),
            Hybi17WsConnection.MaskStrategy.TRANSPARENT_MASK, connectionLogger);
      }


      return new WipTabImpl(socket, browserImpl, listener, description.url());
    }
  }

  private String readHttpResponseContent(InetSocketAddress socketAddress, String resource,
      LoggerFactory loggerFactory) throws IOException {
    ConnectionLogger browserConnectionLogger = loggerFactory.newBrowserConnectionLogger();
    final SocketWrapper socketWrapper = new SocketWrapper(
        socketAddress, DEFAULT_CONNECTION_TIMEOUT_MS, browserConnectionLogger,
        HandshakeUtil.ASCII_CHARSET);
    try {
      if (browserConnectionLogger != null) {
        browserConnectionLogger.start();
        browserConnectionLogger.setConnectionCloser(new ConnectionLogger.ConnectionCloser() {
          @Override
          public void closeConnection() {
            socketWrapper.getShutdownRelay().sendSignal(null, new Exception("UI close request"));
          }
        });
      }
      LoggableOutputStream output = socketWrapper.getLoggableOutput();
      writeHttpLine(output, "GET " + resource + " HTTP/1.1");
      writeHttpLine(output, "User-Agent: ChromeDevTools for Java SDK");
      writeHttpLine(output, "Host: " + socketAddress.getHostName() + ":" +
          socketAddress.getPort());
      writeHttpLine(output, "");
      output.getOutputStream().flush();

      LoggableInputStream input = socketWrapper.getLoggableInput();

      HandshakeUtil.HttpResponse httpResponse =
          HandshakeUtil.readHttpResponse(HandshakeUtil.createLineReader(input.getInputStream()));

      if (httpResponse.getCode() != 200) {
        throw new IOException("Unrecognized respose: " + httpResponse.getCode() + " " +
            httpResponse.getReasonPhrase());
      }

      String lengthStr = httpResponse.getFields().get("content-length");
      if (lengthStr == null) {
        throw new IOException("Unrecognizable respose: no content-length");
      }
      int length;
      try {
        length = Integer.parseInt(lengthStr.trim());
      } catch (NumberFormatException e) {
        throw new IOException("Unrecognizable respose: incorrect content-length");
      }
      byte[] responseBytes = new byte[length];
      {
        int readSoFar = 0;
        while (readSoFar < length) {
          int res = input.getInputStream().read(responseBytes, readSoFar, length - readSoFar);
          if (res == -1) {
            throw new IOException("Unexpected EOS");
          }
          readSoFar += res;
        }
      }
      return new String(responseBytes, HandshakeUtil.UTF_8_CHARSET);
    } finally {
      if (browserConnectionLogger != null) {
        browserConnectionLogger.handleEos();
      }
      socketWrapper.getShutdownRelay().sendSignal(null, null);
    }
  }

  private static void writeHttpLine(LoggableOutputStream output, String line) throws IOException {
    OutputStream stream = output.getOutputStream();
    stream.write(line.getBytes(HandshakeUtil.ASCII_CHARSET));
    stream.write(0xD);
    stream.write(0xA);
  }

  private static List<WipTabList.TabDescription> parseJsonReponse(String content)
      throws IOException {
    Object jsonValue;
    try {
      jsonValue = new JSONParser().parse(content);
    } catch (ParseException e) {
      throw new IOException("Failed to parse a JSON tab list response", e);
    }

    try {
      WipTabList tabList = WipParserAccess.get().parseTabList(jsonValue);
      return tabList.asTabList();
    } catch (JsonProtocolParseException e) {
      throw new IOException(
          "Failed to parse tab list response (on protocol level)", e);
    }
  }
}
