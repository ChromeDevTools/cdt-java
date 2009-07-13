// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.JsStackFrame;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.DebugContext.EvaluateCallback;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * A generic implementation of the JsStackFrame interface.
 */
public class JsStackFrameImpl implements JsStackFrame {

  /** The frame ID as reported by the JavaScript VM. */
  private final int frameId;

  /** The debug context this stack frame belongs in. */
  private final DebugContextImpl context;

  /** The underlying frame data from the JavaScript VM. */
  private final FrameMirror frameMirror;

  /** The variables known in this stack frame. */
  private JsVariableImpl[] variables;

  /**
   * Constructs a stack frame for the given handler using the FrameMirror data
   * from the remote JavaScript VM.
   *
   * @param mirror frame in the VM
   * @param index stack frame id (0 is the stack top)
   * @param context in which the stack frame is created
   */
  public JsStackFrameImpl(FrameMirror mirror, int index, DebugContextImpl context) {
    this.context = context;
    this.frameId = index;
    this.frameMirror = mirror;
  }

  public DebugContextImpl getDebugContext() {
    return context;
  }

  public JsVariableImpl[] getVariables() {
    ensureVariables();
    return variables;
  }

  private void ensureVariables() {
    if (variables == null) {
      this.variables = createVariables();
    }
  }

  public boolean hasVariables() {
    return getVariables().length > 0;
  }

  public int getLineNumber() {
    Script script = frameMirror.getScript();
    // Recalculate respective to the script start
    // (frameMirror.getLine() returns the line offset in the resource).
    return script != null
        ? frameMirror.getLine() - script.getLineOffset()
        : -1;
  }

  public int getCharStart() {
    return -1;
  }

  public int getCharEnd() {
    return -1;
  }

  public String getFunctionName() {
    return frameMirror.getFunctionName();
  }

  public Script getScript() {
    return frameMirror.getScript();
  }

  public void evaluate(final String expression, boolean isSync, final EvaluateCallback callback) {
    DebuggerMessage message =
        DebuggerMessageFactory.evaluate(expression, getIdentifier(), null, null, null);
    BrowserTabImpl.V8HandlerCallback commandCallback = callback == null
        ? null
        : new BrowserTabImpl.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (JsonUtil.isSuccessful(response)) {
              JsVariable variable =
                  new JsVariableImpl(JsStackFrameImpl.this, V8Helper.createValueMirror(
                      JsonUtil.getBody(response), expression));
              if (variable != null) {
                callback.success(variable);
              } else {
                callback.failure("Evaluation failed");
              }
            } else {
              callback.failure(JsonUtil.getAsString(response, V8Protocol.KEY_MESSAGE));
            }
          }

          public void failure(String message) {
            callback.failure(message);
          }
        };
    Exception ex = sendMessage(isSync, message, commandCallback);
    if (ex != null && callback != null) {
      callback.failure(ex.getMessage());
    }
  }

  /**
   * @return this stack frame's unique identifier within the V8 VM (0 is the top
   *         frame)
   */
  int getIdentifier() {
    return frameId;
  }

  /**
   * Initializes this frame with variables based on the frameMirror locals.
   */
  private JsVariableImpl[] createVariables() {
    int numVars = frameMirror.getLocalsCount();
    JsVariableImpl[] result = new JsVariableImpl[numVars];
    for (int i = 0; i < numVars; i++) {
      result[i] = new JsVariableImpl(this, frameMirror.getLocal(i));
    }
    return result;
  }

  /**
   * Sends a DebuggerMessage in a synchronous or asynchronous way.
   *
   * @param isSync whether to send in a synchronous way
   * @param message to send
   * @param commandCallback to invoke
   * @return an Exception that occurred while sending the message synchronously
   *         or {@code null} if sent asynchronously or no error
   */
  private Exception sendMessage(boolean isSync, DebuggerMessage message,
      BrowserTabImpl.V8HandlerCallback commandCallback) {
    if (isSync) {
      return getDebugContext().getV8Handler().sendV8CommandBlocking(
          message, commandCallback);
    } else {
      getDebugContext().getV8Handler().sendV8Command(message, commandCallback);
      return null;
    }
  }

}
