// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import org.chromium.sdk.JsEvaluateContext;

/**
 * Projection of {@link JsEvaluateContext} into Eclipse world.
 */
public class EvaluateContext {
  private final JsEvaluateContext jsEvaluateContext;
  private final JavascriptThread.SuspendedState threadState;

  EvaluateContext(JsEvaluateContext jsEvaluateContext,
      JavascriptThread.SuspendedState threadState) {
    this.jsEvaluateContext = jsEvaluateContext;
    this.threadState = threadState;
  }

  public JsEvaluateContext getJsEvaluateContext() {
    return jsEvaluateContext;
  }

  public JavascriptThread.SuspendedState getThreadSuspendedState() {
    return threadState;
  }
}
