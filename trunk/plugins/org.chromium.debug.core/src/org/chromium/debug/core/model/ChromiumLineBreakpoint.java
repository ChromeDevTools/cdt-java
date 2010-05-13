// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.Breakpoint;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.osgi.util.NLS;

/**
 * JavaScript line breakpoint.
 */
public class ChromiumLineBreakpoint extends LineBreakpoint {

  /** Ignore count */
  private static final String IGNORE_COUNT_ATTR = ChromiumDebugPlugin.PLUGIN_ID + ".ignoreCount"; //$NON-NLS-1$

  /** Condition */
  private static final String CONDITION_ATTR = ChromiumDebugPlugin.PLUGIN_ID + ".condition"; //$NON-NLS-1$

  /**
   * Default constructor is required for the breakpoint manager to re-create
   * persisted breakpoints. After instantiating a breakpoint, the setMarker
   * method is called to restore this breakpoint's attributes.
   */
  public ChromiumLineBreakpoint() {
  }

  /**
   * Constructs a line breakpoint on the given resource at the given line number
   * (line number is 1-based).
   *
   * @param resource file on which to set the breakpoint
   * @param lineNumber 1-based line number of the breakpoint
   * @throws CoreException if unable to create the breakpoint
   */
  public ChromiumLineBreakpoint(final IResource resource, final int lineNumber,
      final String modelId) throws CoreException {
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IMarker marker = resource.createMarker(ChromiumDebugPlugin.BP_MARKER);
        setMarker(marker);
        marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        marker.setAttribute(IBreakpoint.ID, modelId);
        marker.setAttribute(IMarker.MESSAGE, NLS.bind(
            Messages.JsLineBreakpoint_MessageMarkerFormat, resource.getName(), lineNumber));
      }
    };
    run(getMarkerRule(resource), runnable);
  }

  @Override
  public boolean isEnabled() {
    try {
      return super.isEnabled();
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
      return false;
    }
  }

  public void setIgnoreCount(int ignoreCount) {
    setMarkerAttribute(IGNORE_COUNT_ATTR, ignoreCount);
  }

  private void setMarkerAttribute(String attributeName, Object value) {
    try {
      setAttribute(attributeName, value);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  public int getIgnoreCount() {
    return getMarker().getAttribute(IGNORE_COUNT_ATTR, Breakpoint.EMPTY_VALUE);
  }

  public void setCondition(String condition) throws CoreException {
    setMarkerAttribute(CONDITION_ATTR, condition);
  }

  public String getCondition() {
    return getMarker().getAttribute(CONDITION_ATTR, null);
  }

  public String getModelIdentifier() {
    return getMarker().getAttribute(IBreakpoint.ID, "");
  }

  public void changed() {
    BreakpointMap breakpointMap = ChromiumDebugPlugin.getDefault().getBreakpointMap();
    Collection<BreakpointMap.BreakpointMapping> breakpointPairs =
        breakpointMap.getSdkBreakpoints(this);
    for (BreakpointMap.BreakpointMapping pair : breakpointPairs) {
      Breakpoint breakpoint = pair.getSdkBreakpoint();
      try {
        breakpoint.setCondition(getCondition());
        breakpoint.setEnabled(isEnabled());
        breakpoint.setIgnoreCount(getIgnoreCount());
        breakpoint.flush(null);
      } catch (RuntimeException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to change breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(pair.getDebugTarget()), e));
      }
    }
  }

  public void clear() {
    BreakpointMap breakpointMap = ChromiumDebugPlugin.getDefault().getBreakpointMap();
    Collection<BreakpointMap.BreakpointMapping> breakpointPairs =
        breakpointMap.getSdkBreakpoints(this);
    for (BreakpointMap.BreakpointMapping pair : breakpointPairs) {
      Breakpoint breakpoint = pair.getSdkBreakpoint();
      try {
        breakpoint.clear(null);
      } catch (RuntimeException e) {
        ChromiumDebugPlugin.log(new Exception("Failed to remove breakpoint in " + //$NON-NLS-1$
            getTargetNameSafe(pair.getDebugTarget()), e));
      }
    }
  }
  private static String getTargetNameSafe(DebugTargetImpl debugTargetImpl) {
    try {
      return debugTargetImpl.getLaunch().getLaunchConfiguration().getName();
    } catch (RuntimeException e) {
      return "<unknown>"; //$NON-NLS-1$
    }
  }
}
