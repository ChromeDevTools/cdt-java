// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
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

  /**
   * A helper that propagates changes in Eclipse Debugger breakpoints (i.e.
   * {@link ChromiumLineBreakpoint}) to ChromeDevTools SDK breakpoints. Note that
   * {@link ChromiumLineBreakpoint} can't do it itself, because it may correspond to several
   * SDK {@link JavascriptVm}'s simultaneously.
   */
  public static class Helper {
    public interface CreateOnRemoveCallback {
      void success(Breakpoint breakpoint);
      void failure(String errorMessage);
    }

    public static void createOnRemote(ChromiumLineBreakpoint uiBreakpoint,
        Script script, DebugTargetImpl debugTarget,
        final CreateOnRemoveCallback createOnRemoveCallback) throws CoreException {
      JavascriptVm javascriptVm = debugTarget.getJavascriptEmbedder().getJavascriptVm();

      // ILineBreakpoint lines are 1-based while V8 lines are 0-based
      final int line = (uiBreakpoint.getLineNumber() - 1) + script.getStartLine();
      BreakpointCallback callback = new BreakpointCallback() {
        public void success(Breakpoint sdkBreakpoint) {
          createOnRemoveCallback.success(sdkBreakpoint);
        }
        public void failure(String errorMessage) {
          createOnRemoveCallback.failure(errorMessage);
        }
      };

      if (script.getName() != null) {
        javascriptVm.setBreakpoint(Breakpoint.Type.SCRIPT_NAME,
            script.getName(),
            line,
            Breakpoint.EMPTY_VALUE,
            uiBreakpoint.isEnabled(),
            uiBreakpoint.getCondition(),
            uiBreakpoint.getIgnoreCount(),
            callback);
      } else {
        javascriptVm.setBreakpoint(Breakpoint.Type.SCRIPT_ID,
            String.valueOf(script.getId()),
            line,
            Breakpoint.EMPTY_VALUE,
            uiBreakpoint.isEnabled(),
            uiBreakpoint.getCondition(),
            uiBreakpoint.getIgnoreCount(),
            callback);
      }

    }

    public static void updateOnRemote(Breakpoint sdkBreakpoint,
        ChromiumLineBreakpoint uiBreakpoint) {
      sdkBreakpoint.setCondition(uiBreakpoint.getCondition());
      sdkBreakpoint.setEnabled(uiBreakpoint.isEnabled());
      sdkBreakpoint.setIgnoreCount(uiBreakpoint.getIgnoreCount());
      sdkBreakpoint.flush(null);
    }

    public static void removeOnRemote(Breakpoint sdkBreakpoint) {
      sdkBreakpoint.clear(null);
    }
  }
}
