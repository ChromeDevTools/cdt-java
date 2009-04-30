// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.devtools;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.tools.ToolHandler;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.transport.Message;
import org.chromium.debug.core.transport.MessageFactory;
import org.chromium.debug.core.util.JsonUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the interaction with the "DevToolsService" tool.
 */
public class DevToolsServiceHandler extends ToolHandler {

  private volatile ListTabsHandler listTabsHandler;

  public static class TabIdAndUrl {
    public final int id;

    public final String url;

    private TabIdAndUrl(int id, String url) {
      this.id = id;
      this.url = url;
    }
  }

  public interface ListTabsHandler {
    void tabsReceived(List<TabIdAndUrl> tabs);
  }

  public DevToolsServiceHandler(DebugTargetImpl target) {
    super(target);
  }

  @Override
  public void onDebuggerDetached() {
  }

  @Override
  public void handleMessage(Message message) {
    JSONObject json = JsonUtil.jsonObjectFromJson(message.getContent());
    String commandString =
        JsonUtil.getAsString(json, Protocol.KEY_COMMAND);
    DevToolsServiceCommand command =
        DevToolsServiceCommand.forString(commandString);
    if (command != null) {
      int result =
          JsonUtil.getAsLong(json, Protocol.KEY_RESULT).intValue();
      if (result != 0) {
        ChromiumDebugPlugin.logWarning("result={0} from Chromium", result); //$NON-NLS-1$
        return;
      }
      switch (command) {
        case LIST_TABS:
          synchronized (this) {
            if (listTabsHandler != null) {
              ListTabsHandler handler = listTabsHandler;
              listTabsHandler = null;
              JSONArray data =
                  JsonUtil.getAsJSONArray(json, Protocol.KEY_DATA);
              List<TabIdAndUrl> tabs = new ArrayList<TabIdAndUrl>(data.size());
              for (int i = 0; i < data.size(); ++i) {
                JSONArray idAndUrl = (JSONArray) data.get(i);
                int id = ((Long) idAndUrl.get(0)).intValue();
                String url = (String) idAndUrl.get(1);
                tabs.add(new TabIdAndUrl(id, url));
              }
              handler.tabsReceived(tabs);
            }
          }
          break;
        default:
          break;
      }
    }
  }

  public synchronized void listTabs(ListTabsHandler handler) {
    if (listTabsHandler != null) {
      throw new IllegalStateException("list_tabs request is pending"); //$NON-NLS-1$
    }
    listTabsHandler = handler;
    getDebugTarget().getSocketConnection().send(
        MessageFactory.getInstance().listTabs());
  }

  /**
   * This can get called asynchronously.
   */
  public synchronized void resetListTabsHandler() {
    listTabsHandler = null;
  }
}
