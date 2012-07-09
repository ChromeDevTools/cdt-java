// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import static org.junit.Assert.*;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.internal.browserfixture.AbstractAttachedTest;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.junit.Test;

/**
 * A test for the DebugContextImpl class.
 */
public class LoadValueTest extends AbstractAttachedTest<FakeConnection>{

  @Test
  public void testLoadFullValue() throws Exception {
    {
      CountDownLatch latch = expectSuspend();
      messageResponder.hitBreakpoints(Collections.<Long>emptyList());
      latch.await();
    }

    final JsVariable [] expressionResult = { null };
    JsEvaluateContext.EvaluateCallback evaluateCallback = new JsEvaluateContext.EvaluateCallback() {
      public void success(JsVariable variable) {
        expressionResult[0] = variable;
      }
      public void failure(String errorMessage) {
      }
    };

    suspendContext.getGlobalEvaluateContext().evaluateSync("#long_value", null, evaluateCallback);
    assertNotNull(expressionResult[0]);

    JsValue value = expressionResult[0].getValue();
    assertTrue(value.isTruncated());
    String shortValue = value.getValueString();

    final boolean[] reloadResult = { false } ;
    JsValue.ReloadBiggerCallback callback = new JsValue.ReloadBiggerCallback() {
      public void done() {
        reloadResult[0] = true;
      }
    };
    CallbackSemaphore semaphore = new CallbackSemaphore();
    RelayOk relayOk = value.reloadHeavyValue(callback, semaphore);
    semaphore.acquireDefault(relayOk);
    assertTrue(reloadResult[0]);

    String reloadedValue = value.getValueString();

    assertTrue(shortValue.length() < reloadedValue.length());
  }

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }
}
