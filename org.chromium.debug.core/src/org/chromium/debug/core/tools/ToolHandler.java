// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.transport.Message;

/**
 * An abstract implementation of a tool handler to which responses from certain
 * tools are dispatched.
 */
public abstract class ToolHandler {

  private DebugTargetImpl debugTarget;

  public ToolHandler(DebugTargetImpl debugTarget) {
    this.debugTarget = debugTarget;
  }

  public DebugTargetImpl getDebugTarget() {
    return debugTarget;
  }

  /**
   * Handles message from a certain tool.
   */
  abstract public void handleMessage(Message message);

  /**
   * Gets invoked when the debugger has detached from a Chromium instance.
   */
  abstract public void onDebuggerDetached();
}
