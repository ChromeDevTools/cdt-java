// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Breakpoint.Type;
import org.chromium.sdk.BrowserTab.BreakpointCallback;
import org.chromium.sdk.internal.AbstractAttachedTest;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.TestUtil;
import org.chromium.sdk.internal.tools.v8.processor.BreakpointProcessor;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.json.simple.JSONObject;
import org.junit.Test;

/**
 * A BreakpointImpl test.
 */
public class BreakpointImplTest extends AbstractAttachedTest<FakeConnection> {

  public boolean isBreakpointChanged;
  public boolean isBreakpointCleared;

  private class TestBreakpointProcessor extends BreakpointProcessor {

    public TestBreakpointProcessor(DebugContextImpl context) {
      super(context);
    }

    @Override
    public void changeBreakpoint(BreakpointImpl breakpointImpl, BreakpointCallback callback) {
      BreakpointImplTest.this.isBreakpointChanged = true;
    }

    @Override
    public void clearBreakpoint(BreakpointImpl breakpointImpl, BreakpointCallback callback) {
      BreakpointImplTest.this.isBreakpointCleared = true;
    }

    @Override
    public void messageReceived(JSONObject response) {
    }

    @Override
    public void setBreakpoint(Type type, String target, int line, int position, boolean enabled,
        String condition, int ignoreCount, BreakpointCallback callback) {
    }

  }

  @Test
  public void testCreateChange() throws Exception {
    final String[] resultMessage = new String[1];
    final Breakpoint[] resultBreakpoint = new Breakpoint[1];
    Breakpoint breakpoint;
    {
      final CountDownLatch latch = new CountDownLatch(1);

      // The "create" part
      browserTab.setBreakpoint(Type.SCRIPT, "1", 4, 1, true, "false", 3, new BreakpointCallback() {

        @Override
        public void failure(String errorMessage) {
          resultMessage[0] = errorMessage;
          latch.countDown();
        }

        @Override
        public void success(Breakpoint breakpoint) {
          resultBreakpoint[0] = breakpoint;
          latch.countDown();
        }

      });
      latch.await(100, TimeUnit.MILLISECONDS);
      assertNull(resultMessage[0], resultMessage[0]);
      assertNotNull(resultBreakpoint[0]);

      breakpoint = resultBreakpoint[0];
      assertEquals("false", breakpoint.getCondition());
      assertTrue(breakpoint.isEnabled());
      assertEquals(3, breakpoint.getIgnoreCount());
    }

    // The "change" part
    breakpoint.setCondition("true");
    breakpoint.setIgnoreCount(10);
    breakpoint.setEnabled(false);
    resultBreakpoint[0] = null;

    final CountDownLatch latch2 = new CountDownLatch(1);
    breakpoint.flush(new BreakpointCallback() {

      @Override
      public void failure(String errorMessage) {
        resultMessage[0] = errorMessage;
        latch2.countDown();
      }

      @Override
      public void success(Breakpoint breakpoint) {
        resultBreakpoint[0] = breakpoint;
        latch2.countDown();
      }

    });
    latch2.await(100, TimeUnit.MILLISECONDS);
    assertNull(resultMessage[0], resultMessage[0]);
    assertNotNull(resultBreakpoint[0]);
    TestUtil.assertBreakpointsEqual(breakpoint, resultBreakpoint[0]);
  }

  @Test
  public void testClear() throws Exception {
    BreakpointImpl bp = new BreakpointImpl(Type.SCRIPT, 1, true, 0, null,
        new TestBreakpointProcessor((DebugContextImpl) suspendContext));
    final CountDownLatch latch = new CountDownLatch(1);
    final String[] resultMessage = new String[1];
    final Breakpoint[] resultBreakpoint = new Breakpoint[1];

    bp.clear(new BreakpointCallback() {

      @Override
      public void failure(String errorMessage) {
        resultMessage[0] = errorMessage;
        latch.countDown();
      }

      @Override
      public void success(Breakpoint breakpoint) {
        resultBreakpoint[0] = breakpoint;
        latch.countDown();
      }

    });
    latch.await(100, TimeUnit.MILLISECONDS);
    assertNull(resultMessage[0], resultMessage[0]);
    assertTrue(isBreakpointCleared);
  }

  @Test
  public void testNonDirtyChanges() throws Exception {
    TestBreakpointProcessor breakpointProcessor =
        new TestBreakpointProcessor((DebugContextImpl) suspendContext);
    String condition = "true";
    int ignoreCount = 3;
    boolean enabled = true;
    BreakpointImpl bp = new BreakpointImpl(Type.SCRIPT, 1, enabled, ignoreCount, condition,
        breakpointProcessor);

    bp.setCondition(condition);
    bp.flush(null);
    assertFalse(isBreakpointChanged);

    bp.setEnabled(enabled);
    bp.flush(null);
    assertFalse(isBreakpointChanged);

    bp.setIgnoreCount(ignoreCount);
    bp.flush(null);
    assertFalse(isBreakpointChanged);
  }

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }

}
