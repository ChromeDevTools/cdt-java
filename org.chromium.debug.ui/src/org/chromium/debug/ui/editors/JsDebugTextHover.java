// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.ui.actions.ExpressionEvaluator;
import org.chromium.debug.ui.actions.ExpressionEvaluator.EvaluationResult;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

/**
 * Supplies a hover for Javascript expressions while on a breakpoint.
 */
public class JsDebugTextHover implements ITextHover {

  @Override
  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    IDocument doc = textViewer.getDocument();
    int offset = hoverRegion.getOffset();
    String expression =
        JavascriptUtil.extractSurroundingJsIdentifier(doc, offset);
    if (expression == null) {
      return null;
    }
    ExpressionEvaluator evaluator = new ExpressionEvaluator();
    IAdaptable context = DebugUITools.getDebugContext();
    if (context == null) { // debugger not active
      return null;
    }
    StackFrame frame = (StackFrame) context.getAdapter(StackFrame.class);
    if (frame == null) { // not a stackframe-related context
      return null;
    }
    EvaluationResult result;
    try {
      result = evaluator.evaluateSync(expression, frame, null);
    } catch (DebugException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
    if (result == null) {
      return null;
    }
    IValue value = result.getValue();
    if (value == null) {
      return null;
    }
    return JavascriptUtil.stringify(value);
  }

  @Override
  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    IDocument doc = textViewer.getDocument();
    return JavascriptUtil.getSurroundingIdentifierRegion(doc, offset);
  }

}
