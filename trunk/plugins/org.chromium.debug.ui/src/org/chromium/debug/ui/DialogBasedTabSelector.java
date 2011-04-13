// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.model.TabSelector;
import org.chromium.sdk.Browser;
import org.chromium.sdk.Browser.TabConnector;
import org.chromium.sdk.Browser.TabFetcher;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A TabSelector which brings up a dialog allowing users to select which target
 * browser tab to debug.
 */
public class DialogBasedTabSelector implements TabSelector {

  public static DialogBasedTabSelector INSTANCE = new DialogBasedTabSelector();

  public TabConnector selectTab(TabFetcher tabFetcher) throws IOException {
    List<? extends Browser.TabConnector> allTabs = tabFetcher.getTabs();

    List<Browser.TabConnector> filteredTabs = new ArrayList<TabConnector>(allTabs.size());

    for (Browser.TabConnector tab : allTabs) {
      if (!tab.isAlreadyAttached()) {
        filteredTabs.add(tab);
      }
    }

    if (autoSelectSingleTab()) {
      if (allTabs.size() == 1 && filteredTabs.size() == 1) {
        // if all crystal clear -- choose by default
        // disable auto-select if there are some already attached tabs:
        //  user has already seen this dialog and might have got used to it
        //  he might not understand why it didn't show up this time
        return allTabs.get(0);
      }
    }

    final Map<Integer, Browser.TabConnector> map = new HashMap<Integer, Browser.TabConnector>();
    final List<String> urls = new ArrayList<String>(filteredTabs.size());
    for (int i = 0; i < filteredTabs.size(); ++i) {
      Browser.TabConnector connector = filteredTabs.get(i);
      map.put(i, connector);
      urls.add(connector.getUrl());
    }
    final Browser.TabConnector[] result = { null };
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final ChromiumTabSelectionDialog dialog = new ChromiumTabSelectionDialog(shell, urls);
        dialog.setBlockOnOpen(true);
        int dialogResult = dialog.open();
        if (dialogResult == ChromiumTabSelectionDialog.OK) {
          result[0] = map.get(dialog.getSelectedLine());
        }
        // otherwise (result[0] == null) which means "Do not attach"
      }
    });
    return result[0];
  }

  private boolean autoSelectSingleTab() {
    return true;
  }

}
