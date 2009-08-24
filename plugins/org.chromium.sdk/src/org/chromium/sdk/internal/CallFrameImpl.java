// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.chromium.sdk.CallFrame;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * A generic implementation of the CallFrame interface.
 */
public class CallFrameImpl implements CallFrame {

  /** The frame ID as reported by the JavaScript VM. */
  private final int frameId;

  /** The debug context this call frame belongs in. */
  private final InternalContext context;

  /** The underlying frame data from the JavaScript VM. */
  private final FrameMirror frameMirror;

  /** The variables known in this call frame. */
  private Collection<JsVariableImpl> variables;

  private final ContextToken token;

  /**
   * Constructs a call frame for the given handler using the FrameMirror data
   * from the remote JavaScript VM.
   *
   * @param mirror frame in the VM
   * @param index call frame id (0 is the stack top)
   * @param context in which the call frame is created
   * @param contextToken
   */
  public CallFrameImpl(FrameMirror mirror, int index, InternalContext context,
      ContextToken contextToken) {
    this.context = context;
    this.frameId = index;
    this.frameMirror = mirror;
    this.token = contextToken;
  }

  public InternalContext getDebugContext() {
    return context;
  }

  public Collection<JsVariableImpl> getVariables() {
    ensureVariables();
    return variables;
  }

  private void ensureVariables() {
    if (variables == null) {
      this.variables = Collections.unmodifiableCollection(createVariables());
    }
  }

  public boolean hasVariables() {
    return getVariables().size() > 0;
  }

  public int getLineNumber() {
    Script script = frameMirror.getScript();
    // Recalculate respective to the script start
    // (frameMirror.getLine() returns the line offset in the resource).
    return script != null
        ? frameMirror.getLine() - script.getStartLine()
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

  public void evaluateSync(String expression, EvaluateCallback evaluateCallback)
      throws MethodIsBlockingException {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    evaluateAsync(expression, evaluateCallback, callbackSemaphore);
    boolean res = callbackSemaphore.tryAcquireDefault();
    if (!res) {
      evaluateCallback.failure("Timeout");
    }
  }

  public void evaluateAsync(final String expression, final EvaluateCallback callback,
      SyncCallback syncCallback) {
    DebuggerMessage message =
      DebuggerMessageFactory.evaluate(expression, getIdentifier(), null, null, getToken());

    V8CommandProcessor.V8HandlerCallback commandCallback = callback == null
        ? null
        : new V8CommandProcessor.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (JsonUtil.isSuccessful(response)) {
              JsVariable variable =
                  new JsVariableImpl(CallFrameImpl.this, V8Helper.createValueMirror(
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

    getDebugContext().getMessageSender().sendMessageAsync(message, true, commandCallback,
        syncCallback);
  }


  ContextToken getToken() {
    return token;
  }

  /**
   * @return this call frame's unique identifier within the V8 VM (0 is the top
   *         frame)
   */
  int getIdentifier() {
    return frameId;
  }

  /**
   * Initializes this frame with variables based on the frameMirror locals.
   */
  private Collection<JsVariableImpl> createVariables() {
    int numVars = frameMirror.getLocalsCount();
    Collection<JsVariableImpl> result = new ArrayList<JsVariableImpl>(numVars);
    for (int i = 0; i < numVars; i++) {
      result.add(new JsVariableImpl(this, frameMirror.getLocal(i)));
    }
    return result;
  }

}
