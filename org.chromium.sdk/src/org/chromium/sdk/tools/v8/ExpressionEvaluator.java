// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.model.Variable;
import org.chromium.debug.core.tools.v8.BlockingV8RequestCommand;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler.MessageReplyCallback;
import org.chromium.debug.core.tools.v8.model.mirror.Execution;
import org.chromium.debug.core.tools.v8.request.V8Request;
import org.chromium.debug.core.util.JsonUtil;
import org.chromium.debug.ui.ChromiumDebugUIPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.json.simple.JSONObject;

/**
 * A facility that performs expression evaluation in the context of a V8 stack
 * frame.
 */
public class ExpressionEvaluator {

  /**
   * An expression evaluation result.
   */
  public static class EvaluationResult {
    private final String[] errorMessages;
    private final String expression;
    private final IValue value;

    public EvaluationResult(String[] errorMessages, String expression, IValue value) {
      this.errorMessages = errorMessages;
      this.expression = expression;
      this.value = value;
    }

    /**
     * @return error messages that describe the error condition
     */
    public String[] getErrorMessages() {
      return errorMessages;
    }

    /**
     * @return the expression that was evaluated
     */
    public String getEvaluatedExpression() {
      return expression;
    }

    /**
     * @return the evaluated value instance, or null if there were errors
     */
    public IValue getValue() {
      return value;
    }

    /**
     * @return whether errors occurred during evaluation
     */
    public boolean hasErrors() {
      return errorMessages.length > 0;
    }
  }

  /**
   * Clients that wish to be notified of the evaluation completion result should
   * implement this interface.
   */
  public interface Callback {
    /**
     * Gets invoked once the evaluation result becomes available.
     */
    void evaluationComplete(EvaluationResult result);
  }

  private static final String[] EMPTY_STRINGS = new String[0];

  /**
   * Evaluates the expression in the context of the V8 frame using the workbench
   * progress service. The evaluation result is reported to the specified
   * evaluation listener.
   *
   * @param expression
   *          to evaluate
   * @param frame
   *          in whose context to evaluate the expression
   * @param callback
   *          to report the evaluation result to
   * @param shell
   *          may be null in which case the result will not be reported in an
   *          error dialog
   * @throws IllegalArgumentException
   *           if the expression and/or frame is null
   */
  public void evaluate(final String expression, final StackFrame frame,
      final Callback callback, final Shell shell) {
    if (!checkInput(expression, frame, shell)) {
      throw new IllegalArgumentException("Illegal expression and/or frame"); //$NON-NLS-1$
    }

    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {
        if (!frame.isSuspended()) {
          throw new InvocationTargetException(null,
              Messages.ExpressionEvaluator_CannotEvaluateWhenNotSuspended);
        }
        try {
          frame.getHandler().sendV8Command(
              V8Request.evaluate(expression, frame.getIdentifier(), null, null).getMessage(),
                  new MessageReplyCallback() {
                    @Override
                    public void replyReceived(
                        JSONObject reply) {
                      handleEvaluationReply(reply, expression, frame, callback);
                    }
                  });
        } catch (IOException e) {
          callback.evaluationComplete(new EvaluationResult(
              new String[] { e.getMessage() }, expression, null));
        }
      }
    };

    IWorkbench workbench = ChromiumDebugUIPlugin.getDefault().getWorkbench();
    try {
      workbench.getProgressService().busyCursorWhile(runnable);
    } catch (InvocationTargetException e) {
      String message = e.getMessage();
      if (message == null) {
        message = e.getClass().getName();
        if (e.getCause() != null) {
          message = e.getCause().getClass().getName();
          if (e.getCause().getMessage() != null) {
            message = e.getCause().getMessage();
          }
        }
      }
      reportEvaluationError(shell, message);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  /**
   * Synchronously evaluates the given expression in the context of the V8
   * frame.
   *
   * @param expression
   *          to evaluate
   * @param frame
   *          in whose context to evaluate the expression
   * @param shell
   *          may be null in which case the result will not be reported in an
   *          error dialog
   * @throws DebugException
   */
  public EvaluationResult evaluateSync(final String expression,
      final StackFrame frame, final Shell shell) throws DebugException {
    if (!checkInput(expression, frame, shell)) {
      return null;
    }
    final SimpleEvaluationCallback listener = new SimpleEvaluationCallback();
    if (!frame.isSuspended()) {
      return null;
    }
    BlockingV8RequestCommand command =
        new BlockingV8RequestCommand(frame.getHandler(),
            V8Request.evaluate(expression, frame.getIdentifier(), null, null),
            new MessageReplyCallback() {
              @Override
              public void replyReceived(JSONObject reply) {
                handleEvaluationReply(reply, expression, frame, listener);
              }
            });
    command.run();
    if (command.getException() != null) {
      Exception e = command.getException();
      String message = Messages.ExpressionEvaluator_SocketError;
      if (e instanceof InterruptedException) {
        message = Messages.ExpressionEvaluator_EvaluationThreadInterrupted;
      }
      throw new DebugException(new Status(Status.ERROR,
          ChromiumDebugUIPlugin.PLUGIN_ID, message, command.getException()));
    }
    return listener.getEvaluationResult();
  }

  private boolean checkInput(String expression, StackFrame frame, Shell shell) {
    if (expression == null) {
      return false;
    }
    if (frame == null) {
      reportEvaluationError(shell, Messages.ExpressionEvaluator_UnableToEvaluateExpression);
      return false;
    }
    return true;
  }

  private void handleEvaluationReply(JSONObject reply, String selection,
      StackFrame frame, Callback callback) {
    String[] errorMessages = EMPTY_STRINGS;
    IValue value = null;
    Boolean success = JsonUtil.getAsBoolean(reply, Protocol.KEY_SUCCESS);
    if (!success) {
      String message = JsonUtil.getAsString(reply, Protocol.KEY_MESSAGE);
      if (message == null) {
        message = Messages.ExpressionEvaluator_ErrorEvaluatingExpression;
      }
      errorMessages = new String[] { message };
    } else {
      JSONObject handle = JsonUtil.getAsJSON(reply, Protocol.EVAL_BODY);
      Variable var = new Variable(frame, Execution.createValueMirror(handle, selection));
      try {
        value = var.getValue();
      } catch (DebugException e) {
        errorMessages = new String[] { Messages.ExpressionEvaluator_ErrorInspectingObject };
      }
    }
    callback.evaluationComplete(new EvaluationResult(errorMessages,selection, value));
  }

  private void reportEvaluationError(Shell shell, String message) {
    Status status =
        new Status(Status.ERROR, ChromiumDebugUIPlugin.PLUGIN_ID, Status.ERROR,
            message, null);
    if (shell != null) {
      ErrorDialog.openError(shell,
          Messages.ExpressionEvaluator_ErrorEvaluatingExpression, null, status);
    } else {
      ChromiumDebugPlugin.log(status);
    }
  }
}
