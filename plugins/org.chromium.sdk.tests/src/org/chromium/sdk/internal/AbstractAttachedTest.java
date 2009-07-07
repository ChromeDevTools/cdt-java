// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.io.IOException;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.internal.transport.Connection;
import org.junit.After;
import org.junit.Before;

/**
 * A base class for all tests that require an attachment to a browser tab.
 */
public abstract class AbstractAttachedTest<T extends Connection>
    implements DebugEventListener {

  protected FixtureChromeStub messageResponder;

  protected Browser browser;

  protected BrowserTab browserTab;

  protected DebugContext suspendContext;

  protected Runnable suspendCallback;

  protected String newTabUrl;

  protected boolean isDisconnected = false;

  protected T connection;

  @Before
  public void setUpBefore() throws Exception {
    this.newTabUrl = "";
    this.messageResponder = new FixtureChromeStub();
    connection = createConnection();
    attachToBrowserTab();
  }

  protected void attachToBrowserTab() throws IOException, UnsupportedVersionException {
    browser = ((BrowserFactoryImpl) BrowserFactory.getInstance()).create(connection);
    browser.connect();
    BrowserTab[] tabs = browser.getTabs();
    browserTab = tabs[0];
    browserTab.attach(this);
  }

  protected abstract T createConnection();

  protected T getConnection() {
    return connection;
  }

  @After
  public void tearDown() {
    suspendContext = null;
  }

  public void closed() {
    this.newTabUrl = null;
  }

  public void disconnected() {
    this.isDisconnected = true;
  }

  public void navigated(String newUrl) {
    this.newTabUrl = newUrl;
  }

  public void resumed() {
    this.suspendContext = null;
  }

  public void suspended(DebugContext context) {
    this.suspendContext = context;
    if (suspendCallback != null) {
      suspendCallback.run();
    }
  }

}
