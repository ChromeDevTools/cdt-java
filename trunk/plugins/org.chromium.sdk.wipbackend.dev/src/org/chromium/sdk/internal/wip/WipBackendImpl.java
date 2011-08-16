// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.AbstractList;
import java.util.List;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.wip.protocol.WipParserAccess;
import org.chromium.sdk.internal.wip.protocol.input.WipTabList;
import org.chromium.sdk.internal.wip.protocol.input.WipTabList.TabDescription;
import org.chromium.sdk.wip.WipBrowser.WipTabConnector;
import org.chromium.sdk.wip.WipBrowserTab;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WipBackendImpl extends WipBackendBase {
  private static final String ID = "current development";
  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

  private static final String DESCRIPTION =
      "Google Chrome/Chromium: \n" +
      "Chromium build: \n" +
      "WebKit revision: \n";

  public WipBackendImpl() {
    super(ID, DESCRIPTION);
  }

  @Override
  public List<? extends WipTabConnector> getTabs(final WipBrowserImpl browserImpl) throws IOException {
    InetSocketAddress socketAddress = browserImpl.getSocketAddress();

    URL url = new URL("http", socketAddress.getHostName(), socketAddress.getPort(), "/json");
    String content = readURLContent(url);

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
      ConnectionLogger connectionLogger = browserImpl.getConnectionLoggerFactory().newTabConnectionLogger();
      String webSocketDebuggerUrl = description.webSocketDebuggerUrl();

      if (webSocketDebuggerUrl == null) {
        throw new IOException("Tab is already attached");
      }

      URI uri = URI.create(webSocketDebuggerUrl);
      WsConnection socket = WsConnection.connect(browserImpl.getSocketAddress(),
          DEFAULT_CONNECTION_TIMEOUT_MS, uri.getPath(), "empty origin", connectionLogger);

      return new WipTabImpl(socket, browserImpl, listener, description.url());
    }
  }

  private static String readURLContent(URL url) throws IOException {
    Object obj = url.getContent();

    InputStream stream = url.openStream();
    String content;
    try {
      Reader reader = new InputStreamReader(stream, "utf-8");
      StringBuilder stringBuilder = new StringBuilder();
      char[] buffer = new char[1024];
      while (true) {
        int res = reader.read(buffer);
        if (res == -1) {
          break;
        }
        stringBuilder.append(buffer, 0, res);
      }
      content = stringBuilder.toString();
      reader.close();
    } finally {
      stream.close();
    }
    return content;
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
