// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.StepAction;
import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "continue" V8 request message.
 */
public class ContinueRequestMessage extends V8DebugRequestMessage {

  /**
   * @param stepAction
   *          "in", "next", or "out"
   * @param stepCount
   *          number of steps, or {@code null} to use the default (1 step)
   */
  public ContinueRequestMessage(StepAction stepAction, Integer stepCount) {
    super(V8Command.CONTINUE.value);
    if (stepAction != null) {
      putArgument("stepaction", stepAction.value); //$NON-NLS-1$
      putArgument("stepcount", stepCount); //$NON-NLS-1$
    }
  }
}
