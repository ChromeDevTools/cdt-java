// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.BrowserTab.BreakpointCallback;
import org.chromium.sdk.DebugContext.ContinueCallback;
import org.chromium.sdk.internal.transport.FakeConnection;

/**
 * A test for the IVariable implementor.
 */
public class DebugEventListenerTest extends TestCase implements DebugEventListener {

  private FixtureChromeStub messageResponder;

  private BrowserTab browserTab;
  private DebugContext suspendContext;
  private Runnable suspendCallback;
  private String newTabUrl;
  private boolean isDisconnected = false;

  public DebugEventListenerTest() {
    super(DebugEventListenerTest.class.getName());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.messageResponder = new FixtureChromeStub();
    this.newTabUrl = "";
    Browser browser =
      ((BrowserFactoryImpl) BrowserFactory.getInstance()).create(
          new FakeConnection(messageResponder));
    browser.connect();
    BrowserTab[] tabs = browser.getTabs();
    browserTab = tabs[0];
    browserTab.attach(this);
  }

  @Override
  protected void tearDown() throws Exception {
    suspendContext = null;
  }

  public void testDetach() throws Exception {
    assertTrue(browserTab.detach());
    assertFalse(browserTab.isAttached());
    assertTrue(this.isDisconnected);
  }

  public void testSuspendResume() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Breakpoint[] bp = new Breakpoint[1];
    final String[] failure = new String[1];
    browserTab.setBreakpoint(Breakpoint.Type.SCRIPT, "file:///C:/1.js", 18, 3, true, null, 0,
        new BreakpointCallback() {

          @Override
          public void failure(String errorMessage) {
            failure[0] = errorMessage == null ? "" : errorMessage;
            latch.countDown();
          }

          @Override
          public void success(Breakpoint breakpoint) {
            bp[0] = breakpoint;
            latch.countDown();
          }
        });
    latch.await(100, TimeUnit.MILLISECONDS);
    assertNull("Failed to set a breakpoint: " + failure[0], failure[0]);
    assertNotNull("Breakpoint not set", bp[0]);

    messageResponder.hitBreakpoints(Collections.singleton(bp[0].getId()));
    waitForSuspend();
    assertNotNull("suspended() not invoked after the break event", suspendContext);
    Collection<Breakpoint> breakpointsHit = suspendContext.getBreakpointsHit();
    assertEquals(1, breakpointsHit.size());
    Breakpoint bpHit = breakpointsHit.iterator().next();
    assertBreakpointsEqual(bp[0], bpHit);

    resume();

    messageResponder.hitBreakpoints(Collections.<Long>emptySet());
    waitForSuspend();
    assertNotNull("suspended() not invoked after the break event", suspendContext);
    breakpointsHit = suspendContext.getBreakpointsHit();
    assertTrue(breakpointsHit.isEmpty());

    resume();
  }

  private void resume() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final String[] failure = new String[1];
    suspendContext.continueVm(null, 0, new ContinueCallback() {
      public void failure(String errorMessage) {
        failure[0] = errorMessage == null ? "" : errorMessage;
        latch.countDown();
      }

      @Override
      public void success() {
        latch.countDown();
      }
    });
    latch.await(100, TimeUnit.MILLISECONDS);
    assertNull("Failure on continue: " + failure[0], failure[0]);
    assertNull(suspendContext);
  }

  public void testClosed() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    suspendCallback = new Runnable() {
      public void run() {
        latch.countDown();
      }
    };
    messageResponder.tabClosed();
    latch.await(100, TimeUnit.MILLISECONDS);
    assertNull(this.newTabUrl);
  }

  public void testNavigated() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    suspendCallback = new Runnable() {
      public void run() {
        latch.countDown();
      }
    };
    messageResponder.tabNavigated("newUrl");
    latch.await(100, TimeUnit.MILLISECONDS);
    assertEquals("newUrl", this.newTabUrl);
  }

  private static void assertBreakpointsEqual(final Breakpoint bpExpected, Breakpoint bpHit) {
    assertEquals(bpExpected.getId(), bpHit.getId());
    assertEquals(bpExpected.getCondition(), bpHit.getCondition());
    assertEquals(bpExpected.getIgnoreCount(), bpHit.getIgnoreCount());
    assertEquals(bpExpected.getType(), bpHit.getType());
  }

  private void waitForSuspend() throws InterruptedException {
    final CountDownLatch latch2 = new CountDownLatch(1);
    suspendCallback = new Runnable() {
      public void run() {
        latch2.countDown();
      }
    };
    if (!latch2.await(100, TimeUnit.MILLISECONDS)) {
      fail("No \"suspended\" event received");
    }
  }

  @Override
  public void closed() {
    this.newTabUrl = null;
  }

  @Override
  public void disconnected() {
    this.isDisconnected = true;
  }

  @Override
  public void navigated(String newUrl) {
    this.newTabUrl = newUrl;
  }

  @Override
  public void resumed() {
    this.suspendContext = null;
  }

  @Override
  public void suspended(DebugContext context) {
    this.suspendContext = context;
    if (suspendCallback != null) {
      suspendCallback.run();
    }
  }
}
