// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import org.chromium.debug.core.model.StackFrame;
import org.chromium.debug.core.util.JsValueStringifier;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsVariable;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

/**
 * Supplies a hover for JavaScript expressions while on a breakpoint.
 */
public class JsDebugTextHover implements ITextHover {

  private static final JsValueStringifier STRINGIFIER = new JsValueStringifier();

  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    IDocument doc = textViewer.getDocument();
    String expression = JavascriptUtil.extractSurroundingJsIdentifier(doc, hoverRegion.getOffset());
    if (expression == null) {
      return null;
    }

    IAdaptable context = DebugUITools.getDebugContext();
    if (context == null) { // debugger not active
      return null;
    }

    StackFrame frame = (StackFrame) context.getAdapter(StackFrame.class);
    if (frame == null) { // not a stackframe-related context
      return null;
    }

    final JsVariable[] result = new JsVariable[1];
    frame.getCallFrame().getEvaluateContext().evaluateSync(
        expression, new JsEvaluateContext.EvaluateCallback() {
      public void success(JsVariable var) {
        result[0] = var;
      }
      public void failure(String errorMessage) {
      }
    });
    if (result[0] == null) {
      return null;
    }

    return STRINGIFIER.render(result[0].getValue());
  }

  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    IDocument doc = textViewer.getDocument();
    return JavascriptUtil.getSurroundingIdentifierRegion(doc, offset, false);
  }

}
