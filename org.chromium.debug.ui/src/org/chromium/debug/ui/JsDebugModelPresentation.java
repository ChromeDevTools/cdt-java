// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import org.chromium.debug.ui.editors.JsEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * An IDebugModelPresentation for the Chromium Javascript debug model.
 */
public class JsDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {

  public void setAttribute(String attribute, Object value) {
  }

  @Override
  public Image getImage(Object element) {
    // use default image
    return null;
  }

  @Override
  public String getText(Object element) {
    // use default label text
    return null;
  }

  public void computeDetail(IValue value, IValueDetailListener listener) {
    String detail = ""; //$NON-NLS-1$
    try {
      detail = value.getValueString();
    } catch (DebugException e) {
    }

    listener.detailComputed(value, detail);
  }

  public IEditorInput getEditorInput(Object element) {
    if (element instanceof IFile) {
      return new FileEditorInput((IFile) element);
    }

    if (element instanceof ILineBreakpoint) {
      return new FileEditorInput(
          (IFile) ((ILineBreakpoint) element).getMarker().getResource());
    }

    return null;
  }

  public String getEditorId(IEditorInput input, Object element) {
    if (element instanceof IFile || element instanceof ILineBreakpoint) {
      return JsEditor.EDITOR_ID;
    }

    return null;
  }
}
