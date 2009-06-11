// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.devtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.Version;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.MessageFactory;
import org.chromium.sdk.internal.tools.ChromeDevToolsProtocol;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Handles the interaction with the "DevToolsService" tool.
 */
public class DevToolsServiceHandler implements ToolHandler {

  /**
   * The debugger connection to use.
   */
  private final Connection connection;

  /**
   * A "list_tabs" command callback. Is accessed in a synchronized way.
   */
  private ListTabsCallback listTabsCallback;

  /**
   * A "version" command callback. Is accessed in a synchronized way.
   */
  private VersionCallback versionCallback;

  public static class TabIdAndUrl {
    public final int id;

    public final String url;

    private TabIdAndUrl(int id, String url) {
      this.id = id;
      this.url = url;
    }
  }

  /**
   * A callback that will be invoked when the Chrome DevTools protocol version
   * is available.
   */
  private interface VersionCallback {
    void versionReceived(Version version);
  }

  /**
   * A callback that will be invoked when the tabs from the associated browser
   * instance are ready (or not...)
   */
  private interface ListTabsCallback {
    void tabsReceived(List<TabIdAndUrl> tabs);

    void failure(int result);
  }

  public DevToolsServiceHandler(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void onDebuggerDetached() {
  }

  @Override
  public void handleMessage(Message message) {
    JSONObject json;
    try {
      json = JsonUtil.jsonObjectFromJson(message.getContent());
    } catch (ParseException e) {
      Logger.getLogger(DevToolsServiceHandler.class.getName()).log(
          Level.SEVERE, "Invalid JSON received: " + message.getContent());
      return;
    }
    String commandString = JsonUtil.getAsString(json, ChromeDevToolsProtocol.COMMAND.key);
    DevToolsServiceCommand command = DevToolsServiceCommand.forString(commandString);
    if (command != null) {
      switch (command) {
        case LIST_TABS:
          handleListTabs(json);
          break;
        case VERSION:
          handleVersion(json);
          break;
        default:
          break;
      }
    }
  }

  private void handleVersion(JSONObject json) {
    VersionCallback callback = versionCallback;
    if (callback != null) {
      versionCallback = null;
      String versionString = JsonUtil.getAsString(json, ChromeDevToolsProtocol.DATA.key);
      String[] parts = versionString.split("\\.");
      if (parts.length != 2) {
        callback.versionReceived(null); // an invalid version
        return;
      }
      callback.versionReceived(new Version(
          Integer.valueOf(parts[0]),
          Integer.valueOf(parts[1])));
    }
  }

  private void handleListTabs(JSONObject json) {
    ListTabsCallback callback = listTabsCallback;
    if (callback != null) {
      listTabsCallback = null;
      int result = JsonUtil.getAsLong(json, ChromeDevToolsProtocol.RESULT.key).intValue();
      if (result != 0) {
        callback.failure(result);
        return;
      }
      JSONArray data = JsonUtil.getAsJSONArray(json, ChromeDevToolsProtocol.DATA.key);
      List<TabIdAndUrl> tabs = new ArrayList<TabIdAndUrl>(data.size());
      for (int i = 0; i < data.size(); ++i) {
        JSONArray idAndUrl = (JSONArray) data.get(i);
        int id = ((Long) idAndUrl.get(0)).intValue();
        String url = (String) idAndUrl.get(1);
        tabs.add(new TabIdAndUrl(id, url));
      }
      callback.tabsReceived(tabs);
    }
  }

  @SuppressWarnings("unchecked")
  public synchronized List<TabIdAndUrl> listTabs(int timeout) {
    if (listTabsCallback != null) {
      throw new IllegalStateException("list_tabs request is pending");
    }
    final Semaphore sem = new Semaphore(0);
    final List<TabIdAndUrl>[] output = new List[1];
    listTabsCallback = new ListTabsCallback() {
      public void failure(int result) {
        sem.release();
      }

      public void tabsReceived(List<TabIdAndUrl> tabs) {
        output[0] = tabs;
        sem.release();
      }
    };
    connection.send(MessageFactory.listTabs());
    try {
      sem.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Fall through
    }

    if (output[0] == null) {
      return Collections.emptyList();
    }

    return output[0];
  }

  public synchronized Version version(int timeout) {
    if (listTabsCallback != null) {
      throw new IllegalStateException("version request is pending");
    }
    final Semaphore sem = new Semaphore(0);
    final Version[] output = new Version[1];
    versionCallback = new VersionCallback() {
      public void versionReceived(Version version) {
        output[0] = version;
        sem.release();
      }
    };
    connection.send(MessageFactory.version());
    try {
      sem.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Fall through (serverVersion will be null)
    }
    return output[0];
  }

  /**
   * This can get called asynchronously.
   */
  public synchronized void resetListTabsHandler() {
    listTabsCallback = null;
  }
}
