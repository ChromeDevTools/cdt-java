// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "listbreakpoints" V8 request message.
 */
public class ListBreakpointsMessage extends ContextlessDebuggerMessage {

  public ListBreakpointsMessage() {
    super(DebuggerCommand.LISTBREAKPOINTS.value);
  }
}
