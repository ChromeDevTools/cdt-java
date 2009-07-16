// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.model.Value;
import org.chromium.sdk.JsVariable;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IErrorReportingExpression;
import org.eclipse.debug.core.model.IValue;

/**
 * An Eclipse object for the JavaScript inspected expression.
 */
public class JsInspectExpression extends PlatformObject
    implements IErrorReportingExpression, IDebugEventSetListener {

  private final StackFrame stackFrame;

  private final JsVariable variable;

  private final String errorMessage;

  private final String expression;

  public JsInspectExpression(StackFrame stackFrame, String expression, JsVariable variable,
      String errorMessage) {
    this.stackFrame = stackFrame;
    this.expression = expression;
    this.variable = variable;
    this.errorMessage = errorMessage;
  }

  public String[] getErrorMessages() {
    return errorMessage == null
        ? new String[0]
        : new String[] { errorMessage };
  }

  public boolean hasErrors() {
    return errorMessage != null;
  }

  public void dispose() {
  }

  public IDebugTarget getDebugTarget() {
    IValue value = getValue();
    if (value != null) {
      return value.getDebugTarget();
    }
    return null;
  }

  public String getExpressionText() {
    return expression;
  }

  public IValue getValue() {
    return variable != null
        ? Value.create(stackFrame.getDebugTarget(), variable.getValue())
        : null;
  }

  public ILaunch getLaunch() {
    return getValue().getLaunch();
  }

  public String getModelIdentifier() {
    return ChromiumDebugPlugin.DEBUG_MODEL_ID;
  }

  public void handleDebugEvents(DebugEvent[] events) {
    for (DebugEvent event : events) {
      switch (event.getKind()) {
        case DebugEvent.TERMINATE:
          if (event.getSource().equals(getDebugTarget())) {
            DebugPlugin.getDefault().getExpressionManager().removeExpression(this);
          }
          break;
        case DebugEvent.SUSPEND:
          if (event.getDetail() != DebugEvent.EVALUATION_IMPLICIT &&
              event.getSource() instanceof IDebugElement) {
            IDebugElement source = (IDebugElement) event.getSource();
            if (source.getDebugTarget().equals(getDebugTarget())) {
              DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
                  new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.CONTENT) });
            }
          }
          break;
      }
    }
  }

}
