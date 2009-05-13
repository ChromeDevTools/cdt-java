// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.client;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.chromium.sdk.client.V8Debugger;
import org.chromium.sdk.client.V8DebuggerSync;
import org.chromium.sdk.client.response.BreakpointAwareCallback;
import org.chromium.sdk.client.response.BreakpointAwareResponseWrapper;
import org.chromium.sdk.client.response.ContinueCallback;
import org.chromium.sdk.client.response.EvaluateCallback;
import org.chromium.sdk.client.response.EvaluateResponseWrapper;
import org.chromium.sdk.client.response.GenericCallback;
import org.chromium.sdk.client.response.V8ResponseWrapper;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler.ResultAwareCallback;
import org.chromium.sdk.tools.v8.ChromeBreakpoint;
import org.chromium.sdk.tools.v8.ChromeBreakpointType;
import org.chromium.sdk.tools.v8.StepAction;
import org.chromium.sdk.tools.v8.mirror.JsStackFrame;
import org.chromium.sdk.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * A default implementation of the V8Debugger interface.
 */
public class V8DebuggerImpl implements V8Debugger, V8DebuggerSync {

  private final V8DebuggerToolHandler handler;

  public V8DebuggerImpl(V8DebuggerToolHandler handler) {
    this.handler = handler;
  }

  @Override
  public Result attach() {
    final Semaphore sem = new Semaphore(0);
    final Result[] output = new Result[1];
    handler.attachToTab(new ResultAwareCallback() {
      public void resultReceived(Result result) {
        output[0] = result;
        sem.release();
      }
    });
    try {
      sem.acquire();
    } catch (InterruptedException e) {
      // Fall through
    }
    return output[0];
  }

  @Override
  public Result detach() {
    final Semaphore sem = new Semaphore(0);
    final Result[] output = new Result[1];
    handler.detachFromTab(new ResultAwareCallback() {
      public void resultReceived(Result result) {
        output[0] = result;
        sem.release();
      }
    });
    try {
      sem.acquire();
    } catch (InterruptedException e) {
      // Fall through
    }
    return output[0];
  }

  @Override
  public void evaluateJavascript(String javascript) throws IOException {
    handler.sendEvaluateJavascript(javascript);
  }

  @Override
  public void changeBreakpoint(ChromeBreakpoint updatedBreakpoint, final GenericCallback callback) {
    handler.sendV8Command(DebuggerMessageFactory.changeBreakpoint(updatedBreakpoint),
        callback == null ? null : new Callback() {
          public void messageReceived(JSONObject response) {
            callback.responseReceived(new V8ResponseWrapper(response));
          }
        });
  }

  @Override
  public void clearBreakpoint(ChromeBreakpoint breakpoint, final BreakpointAwareCallback callback) {
    clearBreakpointInternal(breakpoint, callback, false);
  }

  @Override
  public void requestContinue(StepAction stepAction, Integer stepCount,
      final ContinueCallback callback) {
    requestContinueInternal(stepAction, stepCount, callback, false);
  }

  @Override
  public void requestEvaluate(String expression, JsStackFrame frame, Boolean global,
      Boolean disableBreak, final EvaluateCallback callback) {
    requestEvaluateInternal(expression, frame, global, disableBreak, callback, false);
  }

  @Override
  public void setBreakpoint(ChromeBreakpointType type, String target, Integer line,
      Integer position, Boolean enabled, String condition, Integer ignoreCount,
      final BreakpointAwareCallback callback) {
    handler.sendV8Command(DebuggerMessageFactory.setBreakpoint(type, target, line, position,
        enabled, condition, ignoreCount), callback == null ? null : new Callback() {
      public void messageReceived(JSONObject response) {
        callback.responseReceived(new BreakpointAwareResponseWrapper(response));
      }
    });
  }

  @Override
  public V8DebuggerSync sync() {
    return this;
  }

  @Override
  public Exception clearBreakpointSync(
      ChromeBreakpoint breakpoint, BreakpointAwareCallback callback) {
    return clearBreakpointInternal(breakpoint, callback, true);
  }

  @Override
  public Exception requestContinueSync(StepAction stepAction, Integer stepCount,
      ContinueCallback callback) {
    return requestContinueInternal(stepAction, stepCount, callback, true);
  }

  @Override
  public Exception requestEvaluateSync(String expression, JsStackFrame frame, Boolean global,
      Boolean disableBreak, EvaluateCallback callback) {
    return requestEvaluateInternal(expression, frame, global, disableBreak, callback, true);
  }

  @Override
  public Exception setBreakpointSync(ChromeBreakpointType type, String target, Integer line,
      Integer position, Boolean enabled, String condition, Integer ignoreCount,
      BreakpointAwareCallback callback) {
    return setBreakpointInternal(type, target, line, position, enabled, condition, ignoreCount,
        callback, true);
  }

  private Exception clearBreakpointInternal(ChromeBreakpoint breakpoint,
      final BreakpointAwareCallback callback, boolean isSync) {
    DebuggerMessage message = DebuggerMessageFactory.clearBreakpoint(breakpoint);
    Callback commandCallback = callback == null ? null : new Callback() {
      public void messageReceived(JSONObject response) {
        callback.responseReceived(new BreakpointAwareResponseWrapper(response));
      }
    };
    return sendMessage(isSync, message, commandCallback);
  }

  private Exception requestContinueInternal(StepAction stepAction, Integer stepCount,
      final ContinueCallback callback, boolean isSync) {
    DebuggerMessage message = DebuggerMessageFactory.goOn(stepAction, stepCount);
    Callback commandCallback = callback == null ? null : new Callback() {
      public void messageReceived(JSONObject response) {
        callback.responseReceived(new V8ResponseWrapper(response));
      }
    };
    return sendMessage(isSync, message, commandCallback);
  }

  private Exception requestEvaluateInternal(String expression, JsStackFrame frame, Boolean global,
      Boolean disableBreak, final EvaluateCallback callback, boolean isSync) {
    DebuggerMessage message =
        DebuggerMessageFactory.evaluate(expression, frame.getIdentifier(), global, disableBreak);
    Callback commandCallback = callback == null ? null : new Callback() {
      public void messageReceived(JSONObject response) {
        callback.responseReceived(new EvaluateResponseWrapper(response));
      }
    };
    return sendMessage(isSync, message, commandCallback);
  }

  private Exception setBreakpointInternal(ChromeBreakpointType type, String target, Integer line,
      Integer position, Boolean enabled, String condition, Integer ignoreCount,
      final BreakpointAwareCallback callback, boolean isSync) {
    DebuggerMessage message =
        DebuggerMessageFactory.setBreakpoint(
            type, target, line, position, enabled, condition, ignoreCount);
    Callback commandCallback = callback == null ? null : new Callback() {
      public void messageReceived(JSONObject response) {
        callback.responseReceived(new BreakpointAwareResponseWrapper(response));
      }
    };
    return sendMessage(isSync, message, commandCallback);
  }

  private Exception sendMessage(boolean isSync, DebuggerMessage message, Callback commandCallback) {
    if (isSync) {
      return handler.sendV8CommandBlocking(message, commandCallback);
    } else {
      handler.sendV8Command(message, commandCallback);
      return null;
    }
  }
}
