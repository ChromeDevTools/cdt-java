// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * An IWorkbenchAdapter factory for JavascriptLineBreakpoints.
 */
public class JsBreakpointWorkbenchAdapterFactory
    implements IAdapterFactory {

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adapterType != IWorkbenchAdapter.class ||
        !(adaptableObject instanceof JsLineBreakpoint)) {
      return null;
    }
    return new IWorkbenchAdapter() {

      public Object[] getChildren(Object o) {
        return null;
      }

      public ImageDescriptor getImageDescriptor(Object object) {
        return null;
      }

      public String getLabel(Object o) {
        JsLineBreakpoint bp = (JsLineBreakpoint) o;
        try {
          return bp.getMarker().getAttribute(IMarker.MESSAGE).toString();
        } catch (CoreException e) {
          ChromiumDebugPlugin.log(e);
        }
        return null;
      }

      public Object getParent(Object o) {
        return null;
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class[] getAdapterList() {
    return new Class[] { IWorkbenchAdapter.class };
  }

}
