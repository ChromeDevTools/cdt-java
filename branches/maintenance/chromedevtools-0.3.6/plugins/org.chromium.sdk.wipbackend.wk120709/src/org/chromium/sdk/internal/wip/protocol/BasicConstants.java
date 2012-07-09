// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol;

public interface BasicConstants {
  interface Property {
    String ID = "id";
    String METHOD = "method";
    String PARAMS = "params";
  }

  interface Domain {
    String DEBUGGER = "Debugger";
    String RUNTIME = "Runtime";
    String INSPECTOR = "Inspector";
    String PAGE = "Page";
    String CONSOLE = "Console";
    String DOM = "DOM";
    String NETWORK = "Network";
  }
}
