// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.BreakpointManager;
import org.chromium.sdk.internal.v8native.ContextBuilder;
import org.chromium.sdk.internal.v8native.DebugSession;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.v8native.protocol.V8Protocol;
import org.chromium.sdk.internal.v8native.protocol.input.BreakEventBody;
import org.chromium.sdk.internal.v8native.protocol.input.EventNotification;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessageFactory;
import org.chromium.sdk.internal.v8native.value.ExceptionDataImpl;
import org.chromium.sdk.internal.v8native.value.ValueLoader;
import org.chromium.sdk.internal.v8native.value.ValueLoaderImpl;
import org.chromium.sdk.internal.v8native.value.ValueMirror;

/**
 * Handles the suspension-related V8 command replies and events.
 */
public class BreakpointProcessor extends V8EventProcessor {

  /** The name of the "exception" object to report as a variable name. */
  private static final String EXCEPTION_NAME = "exception";

  public BreakpointProcessor(DebugSession debugSession) {
    super(debugSession);
  }

  @Override
  public void messageReceived(EventNotification eventMessage) {
    final boolean isEvent = true;
    if (isEvent) {
      String event = eventMessage.event();
      DebugSession debugSession = getDebugSession();

      ContextBuilder contextBuilder = debugSession.getContextBuilder();

      ContextBuilder.ExpectingBreakEventStep step1 = contextBuilder.buildNewContext();

      InternalContext internalContext = step1.getInternalContext();

      BreakEventBody breakEventBody;
      try {
        breakEventBody = eventMessage.body().asBreakEventBody();
      } catch (JsonProtocolParseException e) {
        throw new RuntimeException(e);
      }

      ContextBuilder.ExpectingBacktraceStep step2;
      if (V8Protocol.EVENT_BREAK.key.equals(event)) {
        Collection<Breakpoint> breakpointsHit = getBreakpointsHit(eventMessage, breakEventBody);
        step2 = step1.setContextState(breakpointsHit, null);
      } else if (V8Protocol.EVENT_EXCEPTION.key.equals(event)) {
        ExceptionData exception = createException(eventMessage, breakEventBody,
            internalContext);
        step2 = step1.setContextState(Collections.<Breakpoint> emptySet(), exception);
      } else {
        contextBuilder.buildSequenceFailure();
        throw new RuntimeException();
      }

      processNextStep(step2);
    }
  }

  public void processNextStep(ContextBuilder.ExpectingBacktraceStep step2) {
    BacktraceProcessor backtraceProcessor = new BacktraceProcessor(step2);
    InternalContext internalContext = step2.getInternalContext();

    DebuggerMessage message = DebuggerMessageFactory.backtrace(null, null, true);
    try {
      // Command is not immediate because we are supposed to be suspended.
      internalContext.sendV8CommandAsync(message, false, backtraceProcessor, null);
    } catch (ContextDismissedCheckedException e) {
      // Can't happen -- we are just creating context, it couldn't have become invalid
      throw new RuntimeException(e);
    }
  }

  private Collection<Breakpoint> getBreakpointsHit(EventNotification response,
      BreakEventBody breakEventBody) {
    List<Long> breakpointIdsArray = breakEventBody.breakpoints();
    BreakpointManager breakpointManager = getDebugSession().getBreakpointManager();
    if (breakpointIdsArray == null) {
      // Suspended on step end.
      return Collections.<Breakpoint> emptySet();
    }
    Collection<Breakpoint> breakpointsHit = new ArrayList<Breakpoint>(breakpointIdsArray.size());
    for (int i = 0, size = breakpointIdsArray.size(); i < size; ++i) {
      Breakpoint existingBp = breakpointManager.getBreakpoint(breakpointIdsArray.get(i));
      if (existingBp != null) {
        breakpointsHit.add(existingBp);
      }
    }
    return breakpointsHit;
  }

  private ExceptionData createException(EventNotification response, BreakEventBody body,
      InternalContext internalContext) {
    List<SomeHandle> refs = response.refs();
    ValueHandle exception = body.exception();
    ValueLoaderImpl valueLoader = internalContext.getValueLoader();
    for (SomeHandle handle : refs) {
      valueLoader.addHandleFromRefs(handle);
    }
    valueLoader.addHandleFromRefs(exception.getSuper());

    // source column is not exposed ("sourceColumn" in "body")
    String sourceText = body.sourceLineText();

    ValueMirror mirror = valueLoader.addDataToMap(exception);

    return new ExceptionDataImpl(internalContext,
        mirror,
        EXCEPTION_NAME,
        body.uncaught(),
        sourceText,
        exception.text());
  }

}
