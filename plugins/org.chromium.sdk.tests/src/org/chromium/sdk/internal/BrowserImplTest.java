// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    assertTrue(browser.getPermanentSessionForTest().getBrowserTab(browserTab.getId()).isAttached());
  }

  @Test
  public void checkBrowserIsDisconnectedWhenAllTabsDetached() throws Exception {
    browser.createTabFetcher().getTabs();
    browserTab.detach();
    assertFalse(browser.getPermanentSessionForTest().getConnection().isConnected());
  }
}
