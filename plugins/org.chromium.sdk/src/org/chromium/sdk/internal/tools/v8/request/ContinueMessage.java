// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.DebugContext.StepAction;
import org.chromium.sdk.internal.ContextToken;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "continue" V8 request message.
 */
public class ContinueMessage extends DebuggerMessage {

  private static final Map<StepAction, String> stepActionToV8 = new HashMap<StepAction, String>();

  static {
    stepActionToV8.put(StepAction.IN, "in");
    stepActionToV8.put(StepAction.OUT, "out");
    stepActionToV8.put(StepAction.NEXT, "next");
  }

  /**
   * @param stepAction nullable "in", "next", or "out". Default is "let it go" (do not step)
   * @param stepCount nullable number of steps to perform (positive if not null).
   *        Default is 1 step. Does not make sense when {@code stepAction == null}
   * @param token the context validity token
   */
  public ContinueMessage(StepAction stepAction, Integer stepCount, ContextToken token) {
    super(DebuggerCommand.CONTINUE.value, token);
    if (stepAction != null) {
      putArgument("stepaction", stepActionToV8.get(stepAction));
      putArgument("stepcount", stepCount);
    }
  }
}
