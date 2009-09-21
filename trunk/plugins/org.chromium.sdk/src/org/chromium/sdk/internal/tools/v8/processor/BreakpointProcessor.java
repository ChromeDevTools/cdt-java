// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.internal.ContextBuilder;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.ExceptionDataImpl;
import org.chromium.sdk.internal.InternalContext;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.tools.v8.BreakpointManager;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handles the suspension-related V8 command replies and events.
 */
public class BreakpointProcessor extends V8ResponseCallback {

  /** The name of the "exception" object to report as a variable name. */
  private static final String EXCEPTION_NAME = "exception";

  public BreakpointProcessor(DebugSession debugSession) {
    super(debugSession);
  }

  @Override
  public void messageReceived(JSONObject response) {
    V8MessageType type =
        V8MessageType.forString(JsonUtil.getAsString(response, V8Protocol.KEY_TYPE));
    if (V8MessageType.EVENT == type) {
      String event = JsonUtil.getAsString(response, V8Protocol.KEY_EVENT);
      DebugSession debugSession = getDebugSession();

      ContextBuilder contextBuilder = debugSession.getContextBuilder();

      ContextBuilder.ExpectingBreakEventStep step1 = contextBuilder.buildNewContext();

      InternalContext internalContext = step1.getInternalContext();

      ContextBuilder.ExpectingBacktraceStep step2;
      if (V8Protocol.EVENT_BREAK.key.equals(event)) {
        Collection<Breakpoint> breakpointsHit = getBreakpointsHit(response);
        step2 = step1.setContextState(breakpointsHit, null);
      } else if (V8Protocol.EVENT_EXCEPTION.key.equals(event)) {
        ExceptionData exception = createException(response, internalContext);
        step2 = step1.setContextState(Collections.<Breakpoint> emptySet(), exception);
      } else {
        contextBuilder.buildSequenceFailure();
        throw new RuntimeException();
      }

      BacktraceProcessor backtraceProcessor = new BacktraceProcessor(step2);
      // no need for immediate -- we are known to be on break
      boolean isImmediate = false;
      DebuggerMessage message = DebuggerMessageFactory.backtrace(null, null, true);
      try {
        internalContext.sendMessageAsync(message, isImmediate, backtraceProcessor, null);
      } catch (ContextDismissedCheckedException e) {
        // Can't happen -- we are just creating context, it couldn't have become invalid
        throw new RuntimeException(e);
      }
    }
  }

  private Collection<Breakpoint> getBreakpointsHit(JSONObject reply) {
    JSONObject body = JsonUtil.getAsJSON(reply, V8Protocol.BREAK_BODY);
    JSONArray breakpointIdsArray = JsonUtil.getAsJSONArray(body, V8Protocol.BREAK_BREAKPOINTS);
    BreakpointManager breakpointManager = getDebugSession().getBreakpointManager();
    if (breakpointIdsArray == null) {
      // Suspended on step end.
      return Collections.<Breakpoint> emptySet();
    }
    Collection<Breakpoint> breakpointsHit = new ArrayList<Breakpoint>(breakpointIdsArray.size());
    for (int i = 0, size = breakpointIdsArray.size(); i < size; ++i) {
      Breakpoint existingBp = breakpointManager.getBreakpoint((Long)breakpointIdsArray.get(i));
      if (existingBp != null) {
        breakpointsHit.add(existingBp);
      }
    }
    return breakpointsHit;
  }

  private ExceptionData createException(JSONObject response, InternalContext internalContext) {
    JSONObject body = JsonUtil.getBody(response);

    JSONArray refs = JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS);
    JSONObject exception = JsonUtil.getAsJSON(body, V8Protocol.EXCEPTION);
    Map<Long, JSONObject> refHandleMap = V8ProtocolUtil.getRefHandleMap(refs);
    V8ProtocolUtil.putHandle(refHandleMap, exception);
    internalContext.getHandleManager().putAll(refHandleMap);

    // source column is not exposed ("sourceColumn" in "body")
    String sourceText = JsonUtil.getAsString(body, V8Protocol.BODY_FRAME_SRCLINE);

    return new ExceptionDataImpl(internalContext,
            V8Helper.createValueMirror(exception),
            EXCEPTION_NAME,
            JsonUtil.getAsBoolean(body, V8Protocol.UNCAUGHT),
            sourceText,
            JsonUtil.getAsString(exception, V8Protocol.REF_TEXT));
  }

}
