// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import org.chromium.sdk.internal.ContextToken;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "backtrace" V8 request message.
 */
public class BacktraceMessage extends DebuggerMessage {

  /**
   * @param fromFrame nullable frame range start (0 by default)
   * @param toFrame nullable frame range end (last frame by default)
   * @param inlineRefs whether to inline object refs
   * @param token the context validity token
   */
  public BacktraceMessage(Integer fromFrame, Integer toFrame, boolean inlineRefs,
      ContextToken token) {
    super(DebuggerCommand.BACKTRACE.value, token);
    putArgument("fromFrame", fromFrame);
    putArgument("toFrame", toFrame);
    if (inlineRefs) {
      putArgument("inlineRefs", inlineRefs);
    }
  }
}
