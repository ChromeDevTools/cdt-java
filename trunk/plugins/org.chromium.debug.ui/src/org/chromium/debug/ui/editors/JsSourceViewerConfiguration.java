// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * A JavaScript source viewer configuration.
 */
public class JsSourceViewerConfiguration extends TextSourceViewerConfiguration {

  @Override
  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
    return new JsDebugTextHover();
  }

}
