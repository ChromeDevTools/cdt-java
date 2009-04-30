// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.osgi.util.NLS;

/**
 * Javascript line breakpoint.
 */
public class JsLineBreakpoint extends LineBreakpoint {

  /** Ignore count */
  private static final String IGNORE_COUNT_ATTR =
      ChromiumDebugPlugin.PLUGIN_ID + ".ignoreCount"; //$NON-NLS-1$

  /** Condition */
  private static final String CONDITION_ATTR =
      ChromiumDebugPlugin.PLUGIN_ID + ".condition"; //$NON-NLS-1$

  private int id; // BP ID managed by V8

  /**
   * Default constructor is required for the breakpoint manager to re-create
   * persisted breakpoints. After instantiating a breakpoint, the setMarker
   * method is called to restore this breakpoint's attributes.
   */
  public JsLineBreakpoint() {
  }

  /**
   * Constructs a line breakpoint on the given resource at the given line number
   * (line number is 1-based).
   *
   * @param resource
   *          file on which to set the breakpoint
   * @param lineNumber
   *          1-based line number of the breakpoint
   * @throws CoreException
   *           if unable to create the breakpoint
   */
  public JsLineBreakpoint(
      final IResource resource, final int lineNumber) throws CoreException {
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IMarker marker = resource.createMarker(ChromiumDebugPlugin.BP_MARKER);
        setMarker(marker);
        marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        marker.setAttribute(IBreakpoint.ID, getModelIdentifier());
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

  public void setId(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public void setIgnoreCount(Integer ignoreCount) {
    setMarkerAttribute(
        IGNORE_COUNT_ATTR,
        ignoreCount == null
            ? null
            : String.valueOf(ignoreCount));
  }

  private void setMarkerAttribute(String attributeName, Object value) {
    try {
      setAttribute(attributeName, value);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  public Integer getIgnoreCount() {
    String ignoreCount = getMarker().getAttribute(IGNORE_COUNT_ATTR, null);
    return ignoreCount == null
        ? null
        : Integer.valueOf(ignoreCount);
  }

  public void setCondition(String condition) throws CoreException {
    setMarkerAttribute(CONDITION_ATTR, condition);
  }

  public String getCondition() {
    return getMarker().getAttribute(CONDITION_ATTR, null);
  }

  public String getModelIdentifier() {
    return ChromiumDebugPlugin.DEBUG_MODEL_ID;
  }
}