// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandlerTest;
import org.chromium.sdk.internal.tools.v8.V8Tests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * This class provides all tests for the ChromeDevTools SDK.
 */
@RunWith(Suite.class)
@SuiteClasses({
  BrowserImplTest.class,
  CountingLockTest.class,
  DebugContextImplTest.class,
  DebugEventListenerTest.class,
  JsVariableTest.class,
  V8Tests.class,
  DevToolsServiceHandlerTest.class})
public class AllTests {
}
