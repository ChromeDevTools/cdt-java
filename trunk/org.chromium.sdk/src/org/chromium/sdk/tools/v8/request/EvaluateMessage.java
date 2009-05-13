// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "evaluate" V8 request message.
 */
public class EvaluateRequestMessage extends V8DebugRequestMessage {

  /**
   * @param expression
   *          the expression to evaluate
   * @param frame
   *          the frame number
   * @param global
   * @param disableBreak
   */
  public EvaluateRequestMessage(String expression, Integer frame,
      Boolean global, Boolean disableBreak) {
    super(V8Command.EVALUATE.value);
    putArgument("expression", expression); //$NON-NLS-1$
    putArgument("frame", frame); //$NON-NLS-1$
    putArgument("global", global); //$NON-NLS-1$
    putArgument("disable_break", disableBreak); //$NON-NLS-1$
  }
}
