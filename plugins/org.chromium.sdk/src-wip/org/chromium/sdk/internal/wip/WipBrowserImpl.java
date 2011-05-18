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

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.internal.JavascriptVmImpl;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.wip.protocol.WipParserAccess;
import org.chromium.sdk.internal.wip.protocol.input.WipTabList;
import org.chromium.sdk.internal.wip.protocol.input.WipTabList.TabDescription;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Implements {@link Browser} API that offers connection to a browser tab
 * via WebInspector 'WIP' Protocol.
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
  public WipTabFetcher createTabFetcher() throws IOException, UnsupportedVersionException {
    // You can connect and check version here.

    return new WipTabFetcher() {
      @Override
      public List<? extends WipTabConnector> getTabs() throws IOException,
          IllegalStateException {

        URL url = new URL("http", socketAddress.getHostName(), socketAddress.getPort(), "/json");
        String content = readURLContent(url);

        final List<WipTabList.TabDescription> list = parseJsonReponse(content);

        return new AbstractList<WipTabConnector>() {
          @Override
          public WipTabConnector get(int index) {
            return new TabConnectorImpl(list.get(index));
          }

          @Override
          public int size() {
            return list.size();
          }
        };
      }

      @Override
      public void dismiss() {
      }
    };
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
      throw JavascriptVmImpl.newIOException("Failed to parse a JSON tab list response", e);
    }


    try {
      WipTabList tabList = WipParserAccess.get().parseAnything(jsonValue, WipTabList.class);
      return tabList.asTabList();
    } catch (JsonProtocolParseException e) {
      throw JavascriptVmImpl.newIOException(
          "Failed to parse tab list response (on protocol level)", e);
    }
  }

  private class TabConnectorImpl implements WipTabConnector {
    private final TabDescription description;

    private TabConnectorImpl(TabDescription description) {
      this.description = description;
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
    public BrowserTab attach(TabDebugEventListener listener) throws IOException {
      ConnectionLogger connectionLogger = connectionLoggerFactory.newTabConnectionLogger();

      URI uri = URI.create(description.webSocketDebuggerUrl());
      WsConnection socket = WsConnection.connect(socketAddress,
          DEFAULT_CONNECTION_TIMEOUT_MS, uri.getPath(), "empty origin", connectionLogger);

      return new WipTabImpl(socket, WipBrowserImpl.this, listener, description.url());
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
