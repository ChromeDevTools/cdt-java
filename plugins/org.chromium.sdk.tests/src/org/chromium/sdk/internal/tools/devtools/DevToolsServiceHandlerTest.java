// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.devtools;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import org.chromium.sdk.internal.TestUtil;
import org.chromium.sdk.internal.browserfixture.AbstractAttachedTest;
import org.chromium.sdk.internal.shellprotocol.BrowserImpl;
import org.chromium.sdk.internal.shellprotocol.tools.devtools.DevToolsServiceHandler;
import org.chromium.sdk.internal.transport.ControlledFakeConnection;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for the DevToolsServiceHandler class.
 */
public class DevToolsServiceHandlerTest extends AbstractAttachedTest<ControlledFakeConnection> {

  private DevToolsServiceHandler handler;

  @Before
  public void setUp() throws Exception {
    handler = TestUtil.getDevToolsServiceHandler((BrowserImpl) browserTab.getBrowser());
  }

  @Test(timeout=1000)
  public void verifyListTabsIsExclusive() throws Exception {
    try {
      // immediately after setUp()
      getConnection().setContinuousProcessing(false);
      final Exception[] ex = new Exception[1];
      final CountDownLatch latch = new CountDownLatch(2);
      getTabs(latch, ex, 20);
      getTabs(latch, ex, 20);
      latch.await();
    } catch (IllegalStateException e) {
      fail("'list_tabs' command has exclusive access");
    }
  }

  @Test(timeout=1000)
  public void verifyVersionIsExclusive() throws Exception {
    try {
      // immediately after setUp()
      getConnection().setContinuousProcessing(false);
      final Exception[] ex = new Exception[1];
      final CountDownLatch latch = new CountDownLatch(2);
      getVersion(latch, ex, 20);
      getVersion(latch, ex, 20);
      latch.await();
    } catch (IllegalStateException e) {
      fail("'version' command has exclusive access");
    }
  }

  private void getTabs(final CountDownLatch latch, final Exception[] ex, final int timeout) {
    new MicroThread(new Runnable() {
      public void run() {
        try {
          handler.listTabs(timeout);
        } catch (IllegalStateException e) {
          if (ex != null) {
            ex[0] = e;
          }
        } finally {
          if (latch != null) {
            latch.countDown();
          }
        }
      }
    }).start();
  }

  private void getVersion(final CountDownLatch latch, final Exception[] ex, final int timeout) {
    new MicroThread(new Runnable() {
      public void run() {
        try {
          handler.version(timeout);
        } catch (IllegalStateException e) {
          if (ex != null) {
            ex[0] = e;
          }
        } catch (TimeoutException e) {
          if (ex != null) {
            ex[0] = e;
          }
        } finally {
          if (latch != null) {
            latch.countDown();
          }
        }
      }
    }).start();
  }

  @Override
  protected ControlledFakeConnection createConnection() {
    ControlledFakeConnection conn = new ControlledFakeConnection(messageResponder);
    conn.setContinuousProcessing(true);
    return conn;
  }

  private static class MicroThread extends Thread {

    public MicroThread(Runnable target) {
      super(target);
      setDaemon(true);
    }

  }
}
