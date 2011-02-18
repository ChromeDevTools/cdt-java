// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JsEvaluateContext;

/**
 * Projection of {@link JsEvaluateContext} into Eclipse world.
 */
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

  public DebugContext getDebugContext() {
    return jsEvaluateContext.getDebugContext();
  }
}
