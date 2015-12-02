// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal;

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
  DebugContextImplTest.class,
  DebugEventListenerTest.class,
  JsArrayImplTest.class,
  JsObjectImplTest.class,
  ScriptsTest.class,
  V8Tests.class})
public class AllTests {
}
