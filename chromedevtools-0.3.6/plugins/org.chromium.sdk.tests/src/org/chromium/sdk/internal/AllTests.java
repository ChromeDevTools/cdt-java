// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.internal.browserfixture.BrowserImplTest;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceHandlerTest;
import org.chromium.sdk.internal.tools.v8.V8Tests;
import org.chromium.sdk.internal.v8native.DebugContextImplTest;
import org.chromium.sdk.internal.v8native.DebugEventListenerTest;
import org.chromium.sdk.internal.v8native.ScriptsTest;
import org.chromium.sdk.internal.v8native.value.JsArrayImplTest;
import org.chromium.sdk.internal.v8native.value.JsObjectImplTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * This class provides all tests for the ChromeDevTools SDK.
 */
@RunWith(Suite.class)
@SuiteClasses({
  BrowserImplTest.class,
  DebugContextImplTest.class,
  DebugEventListenerTest.class,
  JsArrayImplTest.class,
  JsObjectImplTest.class,
  ScriptsTest.class,
  V8Tests.class,
  DevToolsServiceHandlerTest.class})
public class AllTests {
}
