// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.Value;
import org.eclipse.debug.core.DebugEvent;

/**
 * The action for context menu in Variable/Expression views that loads full text
 * of string variable if it was truncated initially.
 */
public abstract class LoadFullValueAction extends VariableBasedAction {
  public static class ForVariable extends LoadFullValueAction {
    public ForVariable() {
      super(OpenFunctionAction.VARIABLE_VIEW_ELEMENT_HANDLER);
    }
  }
  public static class ForExpression extends LoadFullValueAction {
    public ForExpression() {
      super(OpenFunctionAction.EXPRESSION_VIEW_ELEMENT_HANDLER);
    }
  }

  protected LoadFullValueAction(ElementHandler elementHandler) {
    super(elementHandler);
  }

  protected Runnable createRunnable(final VariableWrapper wrapper) {
    if (wrapper == null) {
      return null;
    }
    final ConnectedTargetData connectedTargetData = wrapper.getConnectedTargetData();
    if (connectedTargetData == null) {
      return null;
    }
    final Value value = wrapper.getValue();
    if (value == null || !value.isTruncated()) {
      return null;
    }
    return new Runnable() {
      public void run() {
        Value.ReloadValueCallback callback = new Value.ReloadValueCallback() {
          public void done(boolean changed) {
            if (changed) {
              DebugEvent event =
                  new DebugEvent(wrapper.getDebugElement(), DebugEvent.CHANGE, DebugEvent.CONTENT);
              DebugTargetImpl.fireDebugEvent(event);
            }
          }
        };
        value.reloadBiggerValue(callback);
      }
    };
  }
}
