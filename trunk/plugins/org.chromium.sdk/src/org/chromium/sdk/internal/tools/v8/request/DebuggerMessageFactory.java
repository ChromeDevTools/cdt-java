// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import java.util.List;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext.StepAction;
import org.chromium.sdk.internal.ContextToken;

/**
 * A factory for {@link DebuggerMessage}s. Static methods are used to construct
 * commands to be sent to the remote V8 debugger.
 */
public class DebuggerMessageFactory {

  public static DebuggerMessage backtrace(Integer fromFrame, Integer toFrame, boolean compactFormat,
      ContextToken token) {
    return new BacktraceMessage(fromFrame, toFrame, compactFormat, token);
  }

  public static DebuggerMessage goOn(StepAction stepAction, Integer stepCount, ContextToken token) {
    return new ContinueMessage(stepAction, stepCount, token);
  }

  public static DebuggerMessage evaluate(String expression, Integer frame, Boolean global,
      Boolean disableBreak, ContextToken token) {
    return new EvaluateMessage(expression, frame, global, disableBreak, token);
  }

  public static DebuggerMessage frame(Integer frameNumber, ContextToken token) {
    return new FrameMessage(frameNumber, token);
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

  public static DebuggerMessage lookup(List<Long> refs, Boolean inlineRefs, ContextToken token) {
    return new LookupMessage(refs, inlineRefs, token);
  }

  private static Integer getV8IgnoreCount(int count) {
    return count == Breakpoint.NO_VALUE ? null : count;
  }
}
