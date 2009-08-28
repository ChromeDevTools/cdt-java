// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools;

import org.chromium.sdk.internal.transport.Message;

/**
 * An interface of a tool handler to which responses from certain tools are
 * dispatched.
 */
public interface ToolHandler {

  /**
   * Handles message from a certain tool.
   * Invoked from Dispatch thread (connection-driven).
   *
   * @param message to handle. Never null
   */
  void handleMessage(Message message);

  /**
   * Handles EOS (end-of-stream) message. Should not be called twice. Implementation
   * may rely on this in its clean-up work.
   * Invoked from Dispatch thread (connection-driven).
   */
  void handleEos();

  /**
   * Gets invoked when the debugger has detached from a browser instance (due to
   * the connection loss or a user request).
   */
  void onDebuggerDetached();
}
