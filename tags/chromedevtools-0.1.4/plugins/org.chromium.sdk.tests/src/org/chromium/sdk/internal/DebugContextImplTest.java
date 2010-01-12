// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.DebugContext.StepAction;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.junit.Test;

/**
 * A test for the DebugContextImpl class.
 */
public class DebugContextImplTest extends AbstractAttachedTest<FakeConnection>{

  /**
   * Tests the invalidation of the debug context for context-sensitive
   * operations (lookup etc.) on the "continue" request.
   * @throws Exception
   */
  @Test(timeout = 5000)
  public void checkContextIsInvalidatedOnContinue() throws Exception {
    final Breakpoint[] bp = new Breakpoint[1];
    final String[] failure = new String[1];
    {
      final CountDownLatch latch = new CountDownLatch(1);
      browserTab.setBreakpoint(Breakpoint.Type.SCRIPT_NAME, "file:///C:/1.js", 18, 3, true, null, 0,
          new BreakpointCallback() {

            public void failure(String errorMessage) {
              failure[0] = errorMessage == null ? "" : errorMessage;
              latch.countDown();
            }

            public void success(Breakpoint breakpoint) {
              bp[0] = breakpoint;
              latch.countDown();
            }
          });
      latch.await();
    }
    assertNull("Failed to set a breakpoint: " + failure[0], failure[0]);
    assertNotNull("Breakpoint not set", bp[0]);

    {
      CountDownLatch latch = expectSuspend();
      messageResponder.hitBreakpoints(Collections.singleton(bp[0].getId()));
      latch.await();
    }
    Collection<? extends JsVariable> variables =
        suspendContext.getCallFrames().get(0).getVariables();

    // This call invalidates the debug context for the "lookup" operation that is invoked
    // inside "ensureProperties".
    suspendContext.continueVm(StepAction.CONTINUE, 1, null);
    JsObject jsObject = variables.iterator().next().getValue().asObject();
    try {
      jsObject.getProperties();
      fail();
    } catch (RuntimeException e) {
      // this exception is expected
    }
  }

  /**
   * Checks that the debug context for context-sensitive operations
   * (lookup etc.) is valid before sending the "continue" request.
   * @throws Exception
   */
  @Test(timeout = 5000)
  public void checkContextIsValidOffHand() throws Exception {
    final Breakpoint[] bp = new Breakpoint[1];
    final String[] failure = new String[1];
    {
      final CountDownLatch latch = new CountDownLatch(1);
      browserTab.setBreakpoint(Breakpoint.Type.SCRIPT_NAME, "file:///C:/1.js", 18, 3, true, null, 0,
          new BreakpointCallback() {

            public void failure(String errorMessage) {
              failure[0] = errorMessage == null ? "" : errorMessage;
              latch.countDown();
            }

            public void success(Breakpoint breakpoint) {
              bp[0] = breakpoint;
              latch.countDown();
            }
          });
      latch.await();
    }
    assertNull("Failed to set a breakpoint: " + failure[0], failure[0]);
    assertNotNull("Breakpoint not set", bp[0]);

    {
      CountDownLatch latch = expectSuspend();
      messageResponder.hitBreakpoints(Collections.singleton(bp[0].getId()));
      latch.await();
    }
    Collection<? extends JsVariable> variables =
        suspendContext.getCallFrames().get(0).getVariables();

    JsObject jsObject = variables.iterator().next().getValue().asObject();
    // This call should finish OK
    jsObject.getProperties();
  }

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }
}
