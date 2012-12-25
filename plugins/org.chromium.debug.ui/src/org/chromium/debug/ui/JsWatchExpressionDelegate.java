// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugElementImpl;
import org.chromium.debug.core.model.EvaluateContext;
import org.chromium.debug.core.model.ExpressionTracker;
import org.chromium.debug.core.model.Value;
import org.chromium.debug.core.model.ValueBase;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsEvaluateContext.ResultOrException;
import org.chromium.sdk.JsValue;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;

/**
 * Performs the Watch expression evaluation while debugging Chromium JavaScript.
 */
public class JsWatchExpressionDelegate implements IWatchExpressionDelegate {

  private static final String[] EMPTY_STRINGS = new String[0];

  private static final class GoodWatchExpressionResult implements IWatchExpressionResult {
    private final String expression;
    private final ValueBase value;

    GoodWatchExpressionResult(ValueBase value, String expression) {
      this.value = value;
      this.expression = expression;
    }
    @Override public String[] getErrorMessages() {
      return EMPTY_STRINGS;
    }
    @Override public DebugException getException() {
      return null;
    }
    @Override public String getExpressionText() {
      return expression;
    }
    @Override public IValue getValue() {
      return value;
    }
    @Override public boolean hasErrors() {
      return false;
    }
  }

  private static final class BadWatchExpressionResult implements IWatchExpressionResult {

    private final DebugException exception;

    private final String expressionText;

    private BadWatchExpressionResult(DebugException exception, String expressionText) {
      this.exception = exception;
      this.expressionText = expressionText;
    }

    public String[] getErrorMessages() {
      return new String[] { exception.getStatus().getMessage() };
    }

    public DebugException getException() {
      return exception;
    }

    public String getExpressionText() {
      return expressionText;
    }

    public IValue getValue() {
      return null;
    }

    public boolean hasErrors() {
      return true;
    }
  }

  public void evaluateExpression(final String expression, final IDebugElement context,
      final IWatchExpressionListener listener) {
    final DebugElementImpl contextImpl = (DebugElementImpl) context;
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

    final EvaluateContext evaluateContext =
        (EvaluateContext) contextImpl.getAdapter(EvaluateContext.class);
    if (evaluateContext == null) {
      listener.watchEvaluationFinished(new BadWatchExpressionResult(
          new DebugException(new Status(Status.ERROR,
              ChromiumDebugUIPlugin.PLUGIN_ID,"Bad debug context")), //$NON-NLS-1$
          expression));
      return;
    }

    evaluateContext.getJsEvaluateContext().evaluateAsync(
        expression, null,
        new JsEvaluateContext.EvaluateCallback() {
          @Override
          public void success(ResultOrException result) {
            ValueBase valueBase = result.accept(new ResultOrException.Visitor<ValueBase>() {
              @Override public ValueBase visitResult(JsValue value) {
                return Value.create(evaluateContext, value,
                    ExpressionTracker.createExpressionNode(expression));
              }
              @Override
              public ValueBase visitException(JsValue exception) {
                return new ValueBase.ErrorMessageValue(evaluateContext, "<abnormal return>",
                    exception);
              }
            });
            listener.watchEvaluationFinished(new GoodWatchExpressionResult(valueBase, expression));
          }

          @Override
          public void failure(Exception cause) {
            String message = cause.getMessage();
            listener.watchEvaluationFinished(new BadWatchExpressionResult(new DebugException(
                createErrorStatus(message == null
                    ? Messages.JsWatchExpressionDelegate_ErrorEvaluatingExpression
                    : message, null)), expression));
            return;
          }
        },
        null);
  }

  private static Status createErrorStatus(String message, Exception e) {
    return new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID, message, e);
  }

}
