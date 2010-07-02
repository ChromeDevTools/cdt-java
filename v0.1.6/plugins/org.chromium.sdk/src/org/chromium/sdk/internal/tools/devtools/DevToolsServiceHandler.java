// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.devtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.ChromeDevToolsProtocol;
import org.chromium.sdk.internal.tools.ToolHandler;
import org.chromium.sdk.internal.tools.ToolOutput;
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
  private final ToolOutput toolOutput;

  /**
   * A "list_tabs" command callback. Is accessed in a synchronized way.
   */
  private ListTabsCallback listTabsCallback;

  /**
   * A "version" command callback. Is accessed in a synchronized way.
   */
  private VersionCallback versionCallback;

  /**
   * An access/modification lock for the callback fields.
   */
  private final Object lock = new Object();

  public static class TabIdAndUrl {
    public final int id;

    public final String url;

    private TabIdAndUrl(int id, String url) {
      this.id = id;
      this.url = url;
    }

    @Override
    public String toString() {
      return new StringBuilder()
          .append('[')
          .append(id)
          .append('=')
          .append(url)
          .append(']')
          .toString();
    }
  }

  /**
   * A callback that will be invoked when the ChromeDevTools protocol version
   * is available.
   */
  private interface VersionCallback {
    void versionReceived(String versionString);
  }

  /**
   * A callback that will be invoked when the tabs from the associated browser
   * instance are ready (or not...)
   */
  private interface ListTabsCallback {
    void tabsReceived(List<TabIdAndUrl> tabs);

    void failure(int result);
  }

  public DevToolsServiceHandler(ToolOutput toolOutput) {
    this.toolOutput = toolOutput;
  }

  public void onDebuggerDetached() {
  }

  public void handleMessage(Message message) {
    JSONObject json;
    try {
      json = JsonUtil.jsonObjectFromJson(message.getContent());
    } catch (ParseException e) {
      Logger.getLogger(DevToolsServiceHandler.class.getName()).log(
          Level.SEVERE, "Invalid JSON received: {0}", message.getContent());
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

  public void handleEos() {
    // ignore this event, we do not close browser in any way; but clients should dismiss
    // all tickets
  }

  private void handleVersion(JSONObject json) {
    VersionCallback callback;
    synchronized (lock) {
      callback = versionCallback;
      versionCallback = null;
    }
    if (callback != null) {
      String versionString = JsonUtil.getAsString(json, ChromeDevToolsProtocol.DATA.key);
      callback.versionReceived(versionString);
    }
  }

  private void handleListTabs(JSONObject json) {
    ListTabsCallback callback;
    synchronized (lock) {
      callback = listTabsCallback;
      listTabsCallback = null;
    }
    if (callback != null) {
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
  public List<TabIdAndUrl> listTabs(int timeout) {
    final Semaphore sem = new Semaphore(0);
    final List<TabIdAndUrl>[] output = new List[1];
    synchronized (lock) {
      if (listTabsCallback != null) {
        throw new IllegalStateException("list_tabs request is pending");
      }
      listTabsCallback = new ListTabsCallback() {
        public void failure(int result) {
          sem.release();
        }

        public void tabsReceived(List<TabIdAndUrl> tabs) {
          output[0] = tabs;
          sem.release();
        }
      };
    }
    toolOutput.send(CommandFactory.listTabs());
    try {
      if (!sem.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
        resetListTabsHandler();
      }
    } catch (InterruptedException e) {
      // Fall through
    }

    if (output[0] == null) {
      return Collections.emptyList();
    }

    return output[0];
  }

  public String version(int timeout) throws TimeoutException {
    final Semaphore sem = new Semaphore(0);
    final String[] output = new String[1];
    synchronized (lock) {
      if (versionCallback != null) {
        throw new IllegalStateException("version request is pending");
      }
      versionCallback = new VersionCallback() {
        public void versionReceived(String versionString) {
          output[0] = versionString;
          sem.release();
        }
      };
    }
    toolOutput.send(CommandFactory.version());
    boolean res;
    try {
      res = sem.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (!res) {
      throw new TimeoutException("Failed to get version response in " + timeout + " ms");
    }
    return output[0];
  }

  /**
   * This can get called asynchronously.
   */
  public void resetListTabsHandler() {
    synchronized (lock) {
      listTabsCallback = null;
    }
  }

  private static class CommandFactory {

    public static String ping() {
      return createDevToolsMessage(DevToolsServiceCommand.PING);
    }

    public static String version() {
      return createDevToolsMessage(DevToolsServiceCommand.VERSION);
    }
    public static String listTabs() {
      return createDevToolsMessage(DevToolsServiceCommand.LIST_TABS);
    }

    private static String createDevToolsMessage(DevToolsServiceCommand command) {
      return "{\"command\":" + JsonUtil.quoteString(command.commandName) + "}";
    }
  }
}
