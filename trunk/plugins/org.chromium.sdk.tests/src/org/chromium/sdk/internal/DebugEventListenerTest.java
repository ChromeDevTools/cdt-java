// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab.BreakpointCallback;
import org.chromium.sdk.DebugContext.ContinueCallback;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.junit.Test;

/**
 * A test for the DebugEventListener implementor.
 */
public class DebugEventListenerTest extends AbstractAttachedTest<FakeConnection> {

  @Test
  public void testDetach() throws Exception {
    assertTrue(browserTab.detach());
    assertFalse(browserTab.isAttached());
    assertTrue(this.isDisconnected);
  }

  @Test
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
    TestUtil.assertBreakpointsEqual(bp[0], bpHit);

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

  @Test
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

  @Test
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

  private void waitForSuspend() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    suspendCallback = new Runnable() {
      public void run() {
        latch.countDown();
      }
    };
    if (!latch.await(100, TimeUnit.MILLISECONDS)) {
      fail("No \"suspended\" event received");
    }
  }

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }

}
