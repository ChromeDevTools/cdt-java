// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * This class provides
 */
public class AllTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("ChromeDevTools SDK");
    suite.addTestSuite(DebugEventListenerTest.class);
    suite.addTestSuite(JsVariableTest.class);
    return suite;
  }
}
