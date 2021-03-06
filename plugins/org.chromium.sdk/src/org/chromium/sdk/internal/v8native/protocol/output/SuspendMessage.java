// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
