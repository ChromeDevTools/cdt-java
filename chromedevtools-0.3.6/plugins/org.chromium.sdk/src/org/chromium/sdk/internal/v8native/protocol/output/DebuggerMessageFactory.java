// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import java.util.List;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.DebugContext.StepAction;

/**
 * A factory for {@link DebuggerMessage}s. Static methods are used to construct
 * commands to be sent to the remote V8 debugger.
 */
public class DebuggerMessageFactory {

  public static DebuggerMessage backtrace(Integer fromFrame, Integer toFrame,
      boolean compactFormat) {
    return new BacktraceMessage(fromFrame, toFrame, compactFormat);
  }

  public static DebuggerMessage goOn(StepAction stepAction, Integer stepCount) {
    return new ContinueMessage(stepAction, stepCount);
  }

  public static DebuggerMessage evaluate(String expression, Integer frame, Boolean global,
      Boolean disableBreak, List<Map.Entry<String, Integer>> additionalContext) {
    return new EvaluateMessage(expression, frame, global, disableBreak, additionalContext);
  }

  public static DebuggerMessage frame(Integer frameNumber) {
    return new FrameMessage(frameNumber);
  }

  public static ContextlessDebuggerMessage scripts(Integer types, Boolean includeScripts) {
    return new ScriptsMessage(types, includeScripts);
  }

  public static ContextlessDebuggerMessage scripts(List<Long> ids, Boolean includeScripts) {
    return new ScriptsMessage(ids, includeScripts);
  }

  public static ContextlessDebuggerMessage source(Integer frame, Integer fromLine, Integer toLine) {
    return new SourceMessage(frame, fromLine, toLine);
  }

  public static ContextlessDebuggerMessage setBreakpoint(Breakpoint.Target target,
      Integer line, Integer column, Boolean enabled, String condition, Integer ignoreCount) {
    return new SetBreakpointMessage(target, line, column, enabled, condition, ignoreCount);
  }

  public static ContextlessDebuggerMessage clearBreakpoint(long id) {
    return new ClearBreakpointMessage(id);
  }

  public static DebuggerMessage lookup(List<Long> refs, Boolean inlineRefs) {
    return new LookupMessage(refs, inlineRefs);
  }

  public static ContextlessDebuggerMessage suspend() {
    return new SuspendMessage();
  }

  /**
   * A generic 'scope' message parameter that refers to the scope host.
   * It is either a stack frame or a function.
   */
  public static abstract class ScopeHostParameter {
    abstract DebuggerMessage create(int scopeNumber);

    public static ScopeHostParameter forFrame(final int frameNumber) {
      return new ScopeHostParameter() {
        @Override DebuggerMessage create(int scopeNumber) {
          return new ScopeMessage(scopeNumber, frameNumber, null);
        }
      };
    }

    public static ScopeHostParameter forFunction(final long functionHandle) {
      return new ScopeHostParameter() {
        @Override DebuggerMessage create(int scopeNumber) {
          return new ScopeMessage(scopeNumber, null, functionHandle);
        }
      };
    }
  }

  public static DebuggerMessage scope(int scopeNumber, ScopeHostParameter hostParameter) {
    return hostParameter.create(scopeNumber);
  }

  public static ContextlessDebuggerMessage version() {
    return new VersionMessage();
  }
}
