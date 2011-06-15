// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
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

  @Test(timeout = 5000)
  public void testSuspendResume() throws Exception {
    final Breakpoint[] bp = new Breakpoint[1];
    final String[] failure = new String[1];
    {
      final CountDownLatch latch = new CountDownLatch(1);
      Breakpoint.Target target = new Breakpoint.Target.ScriptName("file:///C:/1.js");
      browserTab.setBreakpoint(target, 18, 3, true, null, 0,
          new BreakpointCallback() {

            public void failure(String errorMessage) {
              failure[0] = errorMessage == null ? "" : errorMessage;
              latch.countDown();
            }

            public void success(Breakpoint breakpoint) {
              bp[0] = breakpoint;
              latch.countDown();
            }
          },
          null);
      latch.await();
    }
    assertNull("Failed to set a breakpoint: " + failure[0], failure[0]);
    assertNotNull("Breakpoint not set", bp[0]);

    {
      CountDownLatch latch = expectSuspend();
      messageResponder.hitBreakpoints(Collections.singleton(bp[0].getId()));
      latch.await();
    }

    assertNotNull("suspended() not invoked after the break event", suspendContext);
    Collection<? extends Breakpoint> breakpointsHit = suspendContext.getBreakpointsHit();
    assertEquals(1, breakpointsHit.size());
    Breakpoint bpHit = breakpointsHit.iterator().next();
    TestUtil.assertBreakpointsEqual(bp[0], bpHit);

    resume();

    {
      CountDownLatch latch = expectSuspend();
      messageResponder.hitBreakpoints(Collections.<Long> emptySet());
      latch.await();
    }
    assertNotNull("suspended() not invoked after the break event", suspendContext);
    breakpointsHit = suspendContext.getBreakpointsHit();
    assertTrue(breakpointsHit.isEmpty());

    resume();
  }

  @Test(timeout = 3000)
  public void testClosed() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    closedCallback = new Runnable() {
      public void run() {
        latch.countDown();
      }
    };
    messageResponder.tabClosed();
    latch.await();
    assertNull(this.newTabUrl);
  }

  @Test(timeout = 3000)
  public void testNavigated() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    navigatedCallback = new Runnable() {
      public void run() {
        latch.countDown();
      }
    };
    messageResponder.tabNavigated("newUrl");
    latch.await();
    assertEquals("newUrl", this.newTabUrl);
  }

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }

}
