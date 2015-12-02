// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
