// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.ChromiumExceptionBreakpoint;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.Value;
import org.chromium.debug.core.model.ValueBase;
import org.chromium.debug.core.model.WorkspaceBridge.JsLabelProvider;
import org.chromium.debug.ui.editors.JsEditor;
import org.chromium.sdk.util.BasicUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

/**
 * An IDebugModelPresentation for the Chromium JavaScript debug model.
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
    if (element instanceof DebugTargetImpl) {
      DebugTargetImpl debugTargetImpl = (DebugTargetImpl) element;
      JsLabelProvider labelProvider = debugTargetImpl.getLabelProvider();
      try {
        return labelProvider.getTargetLabel(debugTargetImpl);
      } catch (DebugException e) {
        ChromiumDebugPlugin.log(
            new Exception("Failed to read debug target label", e)); //$NON-NLS-1$
      }
      return null;
    }
    if (element instanceof IBreakpoint) {
      IBreakpoint breakpoint = (IBreakpoint) element;
      ChromiumExceptionBreakpoint exceptionBreakpoint =
          ChromiumExceptionBreakpoint.tryCastBreakpoint(breakpoint);
      if (exceptionBreakpoint != null) {
        String caughtAddendum;
        if (exceptionBreakpoint.getIncludeCaught()) {
          caughtAddendum = Messages.JsDebugModelPresentation_EXCEPTION_LABEL_CAUGHT_ADDENDUM;
        } else {
          caughtAddendum = ""; //$NON-NLS-1$
        }
        return NLS.bind(Messages.JsDebugModelPresentation_EXCEPTION_LABEL,
            caughtAddendum);
      }
      return null;
    }
    // use default label text
    return null;
  }

  public void computeDetail(IValue value, IValueDetailListener listener) {
    ValueBase chromiumValue = ValueBase.cast(value);
    if (chromiumValue == null) {
      String detail = BasicUtil.getStacktraceString(
          new Exception("Unexpected type of value: " + value)); //$NON-NLS-1$
      listener.detailComputed(value, detail);
      return;
    }
    Value realValue = chromiumValue.asRealValue();
    if (realValue == null) {
      String detail = chromiumValue.getValueString();
      listener.detailComputed(value, detail);
      return;
    }
    realValue.computeDetailAsync(listener);
  }

  public IEditorInput getEditorInput(Object element) {
    return toEditorInput(element);
  }

  public static IEditorInput toEditorInput(Object element) {
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
    IFile file;
    if (element instanceof IFile) {
      file = (IFile) element;
    } else if (element instanceof IBreakpoint) {
        IBreakpoint breakpoint = (IBreakpoint) element;
        IResource resource = breakpoint.getMarker().getResource();
        // Can the breakpoint resource be folder or project? Better check for it.
      if (resource instanceof IFile == false) {
        return null;
      }
      file = (IFile) resource;
    } else {
      return null;
    }

    // Pick the editor based on the file extension, taking user preferences into account.
    try {
      return IDE.getEditorDescriptor(file).getId();
    } catch (PartInitException e) {
      // TODO(peter.rybin): should it really be the default case?
      // There might be no virtual project.
      return JsEditor.EDITOR_ID;
    }
  }
}
