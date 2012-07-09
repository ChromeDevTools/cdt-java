// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.browserfixture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chromium.sdk.Browser.TabFetcher;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.junit.Test;

/**
 * A test for the BrowserImpl class.
 */
public class BrowserImplTest extends AbstractAttachedTest<FakeConnection> {

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }

  @Test
  public void checkGetTabsDoesNotResetTabImpls() throws Exception {
    browser.createTabFetcher().getTabs();
    assertTrue(browser.isTabConnectedForTest(browserTab.getId()));
  }

  @Test
  public void checkBrowserIsTabDisconnectedWhenAllTabsDetached() throws Exception {
    TabFetcher fetcher = browser.createTabFetcher();
    fetcher.getTabs();
    fetcher.dismiss();

    browserTab.detach();
    assertFalse(browser.isTabConnectedForTest(browserTab.getId()));
  }

  @Test
  public void checkBrowserIsBrowserDisconnectedWhenAllTabsDetached() throws Exception {
    TabFetcher fetcher = browser.createTabFetcher();
    fetcher.getTabs();
    fetcher.dismiss();

    browserTab.detach();
    assertFalse(browser.isConnectedForTests());
  }
}
