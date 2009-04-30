// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.io.IOException;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugElementImpl;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.model.Variable;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler.MessageReplyCallback;
import org.chromium.debug.core.tools.v8.model.mirror.Execution;
import org.chromium.debug.core.tools.v8.request.V8Request;
import org.chromium.debug.core.util.JsonUtil;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.json.simple.JSONObject;

/**
 * Performs the Watch expression evaluation while debugging Chromium Javascript.
 */
public class JsWatchExpressionDelegate implements IWatchExpressionDelegate {

  private static final String[] EMPTY_STRINGS = new String[0];

  private static final class GoodWatchExpressionResult
      implements IWatchExpressionResult {

    private final Variable variable;

    private final String expression;

    private IValue value;

    private DebugException exception;

    private GoodWatchExpressionResult(Variable variable, String expression) {
      this.variable = variable;
      this.expression = expression;
    }

    @Override
    public String[] getErrorMessages() {
      return exception == null
          ? EMPTY_STRINGS
          : new String[] { exception.getStatus().getMessage() };
    }

    @Override
    public DebugException getException() {
      getValue();
      return exception;
    }

    @Override
    public String getExpressionText() {
      return expression;
    }

    @Override
    public IValue getValue() {
      if (value == null && exception == null) {
        try {
          value = variable.getValue();
        } catch (DebugException e) {
          this.exception = e;
        }
      }
      return value;
    }

    @Override
    public boolean hasErrors() {
      getValue();
      return exception != null;
    }
  }

  private static final class BadWatchExpressionResult implements IWatchExpressionResult {

    private final DebugException exception;

    private final String expressionText;

    private BadWatchExpressionResult(DebugException exception,
        String expressionText) {
      this.exception = exception;
      this.expressionText = expressionText;
    }

    @Override
    public String[] getErrorMessages() {
      return new String[] { exception.getStatus().getMessage() };
    }

    @Override
    public DebugException getException() {
      return exception;
    }

    @Override
    public String getExpressionText() {
      return expressionText;
    }

    @Override
    public IValue getValue() {
      return null;
    }

    @Override
    public boolean hasErrors() {
      return true;
    }
  }

  @Override
  public void evaluateExpression(final String expression,
      final IDebugElement context, final IWatchExpressionListener listener) {
    int frameId = 0; // stack top by default
    final DebugElementImpl contextImpl = (DebugElementImpl) context;
    if (contextImpl instanceof StackFrame) {
      frameId = ((StackFrame) contextImpl).getIdentifier();
    }
    if (!contextImpl.getDebugTarget().isSuspended()) {
      // can only evaluate while suspended. Notify empty result.
      listener.watchEvaluationFinished(new IWatchExpressionResult() {

        public String[] getErrorMessages() {
          return EMPTY_STRINGS;
        }

        public DebugException getException() {
          return null;
        }

        public String getExpressionText() {
          return expression;
        }

        public IValue getValue() {
          return null;
        }

        public boolean hasErrors() {
          return false;
        }
      });
      return;
    }
    try {
      contextImpl.getDebugTarget().getHandler().sendV8Command(
          V8Request.evaluate(expression, frameId, null, null).getMessage(),
          new MessageReplyCallback() {
            public void replyReceived(JSONObject reply) {
              handleResponse(reply, expression, contextImpl, listener);
            }
          });
    } catch (IOException e) {
      listener.watchEvaluationFinished(new BadWatchExpressionResult(
          new DebugException(createErrorStatus(
              Messages.JsWatchExpressionDelegate_ErrorEvaluatingExpression,
              e)), expression));
    }
  }

  private static void handleResponse(JSONObject reply, final String expression,
      DebugElementImpl contextImpl, IWatchExpressionListener listener) {
    boolean success = JsonUtil.getAsBoolean(reply, Protocol.KEY_SUCCESS);
    if (!success) {
      String message = JsonUtil.getAsString(reply, Protocol.KEY_MESSAGE);
      listener.watchEvaluationFinished(new BadWatchExpressionResult(
          new DebugException(createErrorStatus(
              message == null
                  ? Messages.JsWatchExpressionDelegate_ErrorEvaluatingExpression
                  : message,
              null)), expression));
      return;
    }
    int frameId = 0;
    if (contextImpl instanceof StackFrame) {
      frameId = ((StackFrame) contextImpl).getIdentifier();
    }
    StackFrame[] frames =
        contextImpl.getDebugTarget().getHandler().getStackFrames();
    if (frameId >= frames.length) {
      listener.watchEvaluationFinished(new BadWatchExpressionResult(
          new DebugException(createErrorStatus(
              Messages.JsWatchExpressionDelegate_BadStackStructureWhileEvaluating,
              null)), expression));
      return;
    }
    JSONObject handle = JsonUtil.getAsJSON(reply, Protocol.EVAL_BODY);
    final Variable var =
        new Variable(
            frames[frameId], Execution.createValueMirror(handle, expression));
    listener.watchEvaluationFinished(
        new GoodWatchExpressionResult(var, expression));
  }

  private static Status createErrorStatus(String message, Exception e) {
    return new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID, message, e);
  }

}
