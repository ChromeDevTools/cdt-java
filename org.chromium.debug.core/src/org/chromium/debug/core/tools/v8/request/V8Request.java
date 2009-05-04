// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import java.util.List;

import org.chromium.debug.core.model.JsLineBreakpoint;
import org.chromium.debug.core.tools.v8.BreakpointType;
import org.chromium.debug.core.tools.v8.StepAction;

/**
 * V8Request is used to send commands to the remote V8 VM. The static factory
 * methods build instances of V8Request that wrap the corresponding
 * V8DebugRequestMessages.
 */
public class V8Request {
  private final V8DebugRequestMessage message;

  public V8Request(V8DebugRequestMessage message) {
    this.message = message;
  }

  public int getId() {
    return message.getSeq();
  }

  public V8DebugRequestMessage getMessage() {
    return message;
  }

  public static V8Request backtrace(Integer fromFrame, Integer toFrame) {
    return new V8Request(new BacktraceRequestMessage(fromFrame, toFrame));
  }

  public static V8Request goOn(StepAction stepAction, Integer stepCount) {
    return new V8Request(new ContinueRequestMessage(stepAction, stepCount));
  }

  public static V8Request evaluate(String expression, Integer frame,
      Boolean global, Boolean disableBreak) {
    return new V8Request(new EvaluateRequestMessage(expression, frame, global,
        disableBreak));
  }

  public static V8Request frame(Integer frameNumber) {
    return new V8Request(new FrameRequestMessage(frameNumber));
  }

  public static V8Request scripts(Integer types, Boolean includeScripts) {
    return new V8Request(new ScriptsRequestMessage(types, includeScripts));
  }

  public static V8Request source(Integer frame, Integer fromLine, Integer toLine) {
    return new V8Request(new SourceRequestMessage(frame, fromLine, toLine));
  }

  public static V8Request setBreakpoint(BreakpointType type, String target,
      Integer line, Integer position, Boolean enabled, String condition,
      Integer ignoreCount) {
    return new V8Request(new SetBreakpointRequestMessage(type, target, line,
        position, enabled, condition, ignoreCount));
  }

  public static V8Request changeBreakpoint(JsLineBreakpoint breakpoint) {
    return new V8Request(new ChangeBreakpointRequestMessage(breakpoint.getId(),
        breakpoint.isEnabled(), breakpoint.getCondition(), breakpoint.getIgnoreCount()));
  }

  public static V8Request clearBreakpoint(JsLineBreakpoint breakpoint) {
    return new V8Request(new ClearBreakpointRequestMessage(breakpoint.getId()));
  }

  public static V8Request lookup(List<Long> refs) {
    return new V8Request(new LookupRequestMessage(refs));
  }
}
