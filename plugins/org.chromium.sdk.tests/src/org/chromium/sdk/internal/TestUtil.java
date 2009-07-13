// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import junit.framework.Assert;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandler;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler;

/**
 * A utility for performing some common test-related operations.
 */
public class TestUtil {

  public static void assertBreakpointsEqual(Breakpoint bpExpected, Breakpoint bpHit) {
    Assert.assertEquals(bpExpected.getId(), bpHit.getId());
    Assert.assertEquals(bpExpected.getCondition(), bpHit.getCondition());
    Assert.assertEquals(bpExpected.getIgnoreCount(), bpHit.getIgnoreCount());
    Assert.assertEquals(bpExpected.getType(), bpHit.getType());
  }

  public static DevToolsServiceHandler getDevToolsServiceHandler(BrowserImpl browserImpl) {
    return browserImpl.getDevToolsServiceHandler();
  }

  public static V8DebuggerToolHandler getV8DebuggerToolHandler(BrowserTabImpl browserTabImpl) {
    return browserTabImpl.getDebugContext().getV8Handler();
  }

  private TestUtil() {
    // not instantiable
  }
}
