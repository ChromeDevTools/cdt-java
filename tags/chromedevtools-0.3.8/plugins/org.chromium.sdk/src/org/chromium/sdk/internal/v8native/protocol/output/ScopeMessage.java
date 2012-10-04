// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "scope" request message.
 */
public class ScopeMessage extends DebuggerMessage {

  public ScopeMessage(int scopeNumber, Integer frameNumber, Long functionHandle) {
    super(DebuggerCommand.SCOPE.value);
    putArgument("number", scopeNumber);
    if (frameNumber != null) {
      putArgument("frameNumber", frameNumber);
    }
    if (functionHandle != null) {
      putArgument("functionHandle", functionHandle);
    }
    putArgument("inlineRefs", true);
  }

}
