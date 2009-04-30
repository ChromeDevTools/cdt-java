// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import org.chromium.debug.ui.actions.ExpressionEvaluator.Callback;
import org.chromium.debug.ui.actions.ExpressionEvaluator.EvaluationResult;

/**
 * A simplistic implementation of ExpressionEvaluator.Callback.
 */
public class SimpleEvaluationCallback implements Callback {

  private EvaluationResult result;

  @Override
  public void evaluationComplete(EvaluationResult result) {
    this.result = result;
  }

  public EvaluationResult getEvaluationResult() {
    return result;
  }
}