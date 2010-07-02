// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import java.util.EnumMap;
import java.util.Map;

import org.chromium.sdk.DebugContext.StepAction;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "continue" V8 request message.
 */
public class ContinueMessage extends DebuggerMessage {

  private static final Map<StepAction, String> stepActionToV8 =
      new EnumMap<StepAction, String>(StepAction.class);

  static {
    stepActionToV8.put(StepAction.IN, "in");
    stepActionToV8.put(StepAction.OUT, "out");
    stepActionToV8.put(StepAction.OVER, "next");
    stepActionToV8.put(StepAction.CONTINUE, null);
  }

  /**
   * @param stepAction the kind of step to perform
   * @param stepCount nullable number of steps to perform (positive if not null).
   *        Default is 1 step. Not used when {@code stepAction == CONTINUE}
   */
  public ContinueMessage(StepAction stepAction, Integer stepCount) {
    super(DebuggerCommand.CONTINUE.value);
    String stepActionString = stepActionToV8.get(stepAction);
    if (stepActionString != null) {
      putArgument("stepaction", stepActionString);
      putArgument("stepcount", stepCount);
    }
  }
}
