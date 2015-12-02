// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.tests.system.runner;

import java.io.File;

/**
 * API to platform-specific routines that are not implemented as Java code and
 * to local path to SDK test kit.
 */
public interface ICustom {
  ICustom INSTANCE = new Custom();

  File downloadChrome(String url);

  void runTestKit(String testArgument, int port);

  String getKitWebPageUrl();
}
