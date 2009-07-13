// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab.BreakpointCallback;
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
    final CountDownLatch latch = new CountDownLatch(1);
    final Breakpoint[] bp = new Breakpoint[1];
    final String[] failure = new String[1];
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
    assertNull("Failed to set a breakpoint: " + failure[0], failure[0]);
    assertNotNull("Breakpoint not set", bp[0]);

    messageResponder.hitBreakpoints(Collections.singleton(bp[0].getId()));
    waitForSuspend();
    JsVariableImpl[] variables = suspendContext.getStackFrames()[0].getVariables();

    // This call invalidates the debug context for the "lookup" operation that is invoked
    // inside "ensureProperties".
    suspendContext.continueVm(null, 1, null);
    JsObjectImpl jsObject = variables[0].getValue().asObject();
    jsObject.ensureProperties();
    assertTrue(jsObject.isFailedResponse());
  }

  /**
   * Checks that the debug context for context-sensitive operations
   * (lookup etc.) is valid before sending the "continue" request.
   * @throws Exception
   */
  @Test(timeout = 5000)
  public void checkContextIsValidOffHand() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Breakpoint[] bp = new Breakpoint[1];
    final String[] failure = new String[1];
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
    assertNull("Failed to set a breakpoint: " + failure[0], failure[0]);
    assertNotNull("Breakpoint not set", bp[0]);

    messageResponder.hitBreakpoints(Collections.singleton(bp[0].getId()));
    waitForSuspend();
    JsVariableImpl[] variables = suspendContext.getStackFrames()[0].getVariables();

    JsObjectImpl jsObject = variables[0].getValue().asObject();
    jsObject.ensureProperties();
    assertFalse(jsObject.isFailedResponse());
  }

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }
}
