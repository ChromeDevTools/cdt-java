// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "suspend" V8 request message.
 */
class SuspendMessage extends ContextlessDebuggerMessage {
  SuspendMessage() {
    super(DebuggerCommand.SUSPEND.value);
  }
}
