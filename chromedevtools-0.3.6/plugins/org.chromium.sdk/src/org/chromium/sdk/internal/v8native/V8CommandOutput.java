// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;

/**
 * Abstract sink for DebuggerMessage v8 messages. It is responsible for sending them to a
 * particular instance of V8 VM. For this end actual message may get additional fields or
 * be reformatted.
 */
public interface V8CommandOutput {
  void send(DebuggerMessage debuggerMessage, boolean immediate);

  /**
   * Asynchronously runs the callback in Connection's Dispatch thread.
   */
  void runInDispatchThread(Runnable callback);
}
