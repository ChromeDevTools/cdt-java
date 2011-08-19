// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
