// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.ui.actions.ExpressionEvaluator.EvaluationResult;
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
 * An Eclipse object for the Javascript inspected expression.
 */
public class JsInspectExpression extends PlatformObject
    implements IErrorReportingExpression, IDebugEventSetListener {

  private final EvaluationResult result;

  public JsInspectExpression(EvaluationResult result) {
    this.result = result;
  }

  @Override
  public String[] getErrorMessages() {
    return result.getErrorMessages();
  }

  @Override
  public boolean hasErrors() {
    return result.hasErrors();
  }

  @Override
  public void dispose() {
  }

  @Override
  public IDebugTarget getDebugTarget() {
    IValue value = getValue();
    if (value != null) {
      return value.getDebugTarget();
    }
    return null;
  }

  @Override
  public String getExpressionText() {
    return result.getEvaluatedExpression();
  }

  @Override
  public IValue getValue() {
    return result.getValue();
  }

  @Override
  public ILaunch getLaunch() {
    return getValue().getLaunch();
  }

  @Override
  public String getModelIdentifier() {
    return ChromiumDebugPlugin.DEBUG_MODEL_ID;
  }

  @Override
  public void handleDebugEvents(DebugEvent[] events) {
    for (DebugEvent event : events) {
      switch (event.getKind()) {
        case DebugEvent.TERMINATE:
          if (event.getSource().equals(getDebugTarget())) {
            DebugPlugin.getDefault().getExpressionManager().removeExpression(this);
          }
          break;
        case DebugEvent.SUSPEND:
          if (event.getDetail() != DebugEvent.EVALUATION_IMPLICIT) {
            if (event.getSource() instanceof IDebugElement) {
              IDebugElement source = (IDebugElement) event.getSource();
              if (source.getDebugTarget().equals(getDebugTarget())) {
                DebugPlugin.getDefault().fireDebugEventSet(
                    new DebugEvent[] {
                        new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.CONTENT) });
              }
            }
          }
          break;
      }
    }
  }

}
