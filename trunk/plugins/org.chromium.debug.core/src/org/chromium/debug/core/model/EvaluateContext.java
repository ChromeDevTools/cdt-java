package org.chromium.debug.core.model;

import org.chromium.sdk.JsEvaluateContext;

public class EvaluateContext {
  private final JsEvaluateContext jsEvaluateContext;
  private final DebugTargetImpl debugTargetImpl;

  EvaluateContext(JsEvaluateContext jsEvaluateContext, DebugTargetImpl debugTargetImpl) {
    this.jsEvaluateContext = jsEvaluateContext;
    this.debugTargetImpl = debugTargetImpl;
  }

  public JsEvaluateContext getJsEvaluateContext() {
    return jsEvaluateContext;
  }

  public DebugTargetImpl getDebugTarget() {
    return debugTargetImpl;
  }
}
