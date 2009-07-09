// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import java.util.List;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext.StepAction;

/**
 * A factory for {@link DebuggerMessage}s. Static methods are used to construct
 * commands to be sent to the remote V8 debugger.
 */
public class DebuggerMessageFactory {

  public static DebuggerMessage backtrace(Integer fromFrame, Integer toFrame, boolean compactFormat) {
    return new BacktraceMessage(fromFrame, toFrame, compactFormat);
  }

  public static DebuggerMessage goOn(StepAction stepAction, Integer stepCount) {
    return new ContinueMessage(stepAction, stepCount);
  }

  public static DebuggerMessage evaluate(String expression, Integer frame, Boolean global,
      Boolean disableBreak) {
    return new EvaluateMessage(expression, frame, global, disableBreak);
  }

  public static DebuggerMessage frame(Integer frameNumber) {
    return new FrameMessage(frameNumber);
  }

  public static DebuggerMessage scripts(Integer types, Boolean includeScripts) {
    return new ScriptsMessage(types, includeScripts);
  }

  public static DebuggerMessage scripts(List<Long> ids, Boolean includeScripts) {
    return new ScriptsMessage(ids, includeScripts);
  }

  public static DebuggerMessage source(Integer frame, Integer fromLine, Integer toLine) {
    return new SourceMessage(frame, fromLine, toLine);
  }

  public static DebuggerMessage setBreakpoint(Breakpoint.Type type, String target,
      Integer line, Integer position, Boolean enabled, String condition, Integer ignoreCount) {
    return new SetBreakpointMessage(type, target, line, position, enabled, condition, ignoreCount);
  }

  public static DebuggerMessage changeBreakpoint(Breakpoint breakpoint) {
    return new ChangeBreakpointMessage(breakpoint.getId(), breakpoint.isEnabled(),
        breakpoint.getCondition(), getV8IgnoreCount(breakpoint.getIgnoreCount()));
  }

  public static DebuggerMessage clearBreakpoint(Breakpoint breakpoint) {
    return new ClearBreakpointMessage(breakpoint.getId());
  }

  public static DebuggerMessage lookup(List<Long> refs, Boolean inlineRefs) {
    return new LookupMessage(refs, inlineRefs);
  }

  private static Integer getV8IgnoreCount(int count) {
    return count == Breakpoint.NO_VALUE ? null : count;
  }
}
