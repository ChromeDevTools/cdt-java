// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda.protocol;

public interface BasicConstants {
  interface Property {
    String DOMAIN = "domain";
    String ARGUMENTS = "agruments";
    String SEQ = "seq";
    String COMMAND = "command";
    String EVENT = "event";
  }

  interface Domain {
    String DEBUGGER = "Debugger";
    String RUNTIME = "Runtime";
  }
}
