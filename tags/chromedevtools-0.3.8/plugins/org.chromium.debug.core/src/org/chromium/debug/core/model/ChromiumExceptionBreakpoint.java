// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.Breakpoint;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * JavaScript exception breakpoint. It always stops for uncaught breakpoints,
 * and optionally stops on caught breakpoints (see 'include caught' property).
 */
public class ChromiumExceptionBreakpoint extends Breakpoint {
  /** Include caught */
  private static final String INCLUDE_CAUGHT_ATTR =
      ChromiumDebugPlugin.PLUGIN_ID + ".includeCaught"; //$NON-NLS-1$

  public ChromiumExceptionBreakpoint() {
  }

  public ChromiumExceptionBreakpoint(final IResource resource,
      final boolean includingCaught, final String modelId) throws DebugException {
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IMarker marker = resource.createMarker(ChromiumDebugPlugin.EXCEPTION_BP_MARKER);
        setMarker(marker);
        marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
        marker.setAttribute(IBreakpoint.ID, modelId);
        marker.setAttribute(IMarker.MESSAGE,
            Messages.ChromiumExceptionBreakpoint_MessageMarkerFormat);
      }
    };
    run(getMarkerRule(resource), runnable);
  }

  public void setIncludeCaught(boolean value) throws CoreException {
    try {
      setAttribute(INCLUDE_CAUGHT_ATTR, value);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  public boolean getIncludeCaught() {
    return getMarker().getAttribute(INCLUDE_CAUGHT_ATTR, false);
  }

  @Override
  public String getModelIdentifier() {
    return getMarker().getAttribute(IBreakpoint.ID, ""); //$NON-NLS-1$
  }

  public static ChromiumExceptionBreakpoint tryCastBreakpoint(IBreakpoint breakpoint) {
    if (breakpoint instanceof ChromiumExceptionBreakpoint == false) {
      return null;
    }
    return (ChromiumExceptionBreakpoint) breakpoint;
  }
}
