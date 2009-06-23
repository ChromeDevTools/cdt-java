// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import static org.junit.Assert.*;

import org.chromium.sdk.internal.AbstractAttachedTest;
import org.chromium.sdk.internal.BrowserTabImpl;
import org.chromium.sdk.internal.MessageFactory;
import org.chromium.sdk.internal.tools.ToolName;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for the V8DebuggerToolHandler class.
 */
public class V8DebuggerToolHandlerTest extends AbstractAttachedTest<FakeConnection> {

  private V8DebuggerToolHandler handler;

  @Before
  public void setUp() throws Exception {
    this.handler = ((BrowserTabImpl) browserTab).getDebugContext().getV8Handler();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadCommand() throws Exception {
    handler.handleMessage(MessageFactory.createMessage(ToolName.V8_DEBUGGER.value, null,
        "{\"command\":\"badcommand\"}"));
    fail("'badcommand' considered a valid command");
  }

  @Test
  public void testDebuggerDetached() {
    assertTrue(browserTab.isAttached());
    getConnection().close(); // results in handler.onDebuggerDetached()
    assertFalse(browserTab.isAttached());
    assertTrue(isDisconnected);
  }

  @Override
  protected FakeConnection createConnection() {
    return new FakeConnection(messageResponder);
  }

}
