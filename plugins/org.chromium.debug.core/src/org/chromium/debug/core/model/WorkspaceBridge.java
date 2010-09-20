// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.model.IBreakpoint;


/**
 * This interface draws a connection between V8 debug target and Eclipse workspace with its
 * resources. Instance of this interface corresponds to a particular target. It is oriented
 * to "virtual project" model, when all source files are virtual and downloaded right from
 * running V8. "Native" model, when all source files are regular files of Eclipse workspace
 * (and are only supposed to correspond to scripts that are actually running in remote V8)
 * is also supported and assumes stub implementations for most of methods.
 */
public interface WorkspaceBridge {

  /**
   * A factory interface for {@link WorkspaceBridge}. It seems to tend to become
   * a workspace relations type object, that is not tied to a particular {@link DebugTargetImpl},
   * but so far it only has 1 additional method.
   */
  interface Factory {
    /**
     * Creates new instance of {@link WorkspaceBridge} when connection to V8 VM is already
     * established.
     */
    WorkspaceBridge attachedToVm(DebugTargetImpl debugTargetImpl, JavascriptVm javascriptVm);

    String getDebugModelIdentifier();

    /**
     * Since we define here how scripts are mapped to workspace, we may want to specify
     * labels in UI. Each type of bridges provides its own label providers here.
     * User may cache value of this method.
     */
    JsLabelProvider getLabelProvider();
  }

  VmResource findVmResourceFromWorkspaceFile(IFile resource) throws CoreException;

  /**
   * Called after session has been started. It should start process of debug session initialization
   * (downloading scripts from remove, synchronizing breakpoints etc).
   * This method should be non-blocking.
   */
  void startInitialization();

  /**
   * Initiates script reloading from remote VM.
   */
  void reloadScript(Script script);

  /**
   * Reports about new script loaded in JavaScript VM.
   */
  void scriptLoaded(Script newScript);

  /**
   * Reports about the script having been collected and disposed in JavaScript VM.
   */
  void scriptCollected(Script script);

  /**
   * Handles reset event in JavaScript VM (e.g. Chromium tab reload or navigate event).
   * The whole context changes.
   */
  void handleVmResetEvent();

  /**
   * Detach command is about to be sent to JavaScript VM.
   */
  void beforeDetach();

  /**
   * Virtual project is expected to stay live until launch is removed from Launches view. Then
   * it has to go.
   */
  void launchRemoved();

  /**
   * Returns instance of breakpoint handler. Should be a simple getter, caller may cache result
   * value.
   */
  BreakpointHandler getBreakpointHandler();

  /**
   * Breakpoint-related aspect of {@link WorkspaceBridge} interface.
   */
  interface BreakpointHandler extends IBreakpointListener, IBreakpointManagerListener {
    boolean supportsBreakpoint(IBreakpoint breakpoint);
    void breakpointsHit(Collection<? extends Breakpoint> breakpointsHit);
    void initBreakpointManagerListenerState(IBreakpointManager breakpointManager);

    Boolean getBreakExceptionState(JavascriptVm.ExceptionCatchType catchType);
    void setBreakExceptionState(JavascriptVm.ExceptionCatchType catchType, boolean value);
  }

  /**
   * Label provider for several debug elements. This object should be stateless.
   */
  interface JsLabelProvider {
    /**
     * Label for the debug target to be shown in the Debug view.
     */
    String getTargetLabel(DebugTargetImpl debugTarget) throws DebugException;

    /**
     * Label for JavaScript thread to be shown in the Debug view.
     */
    String getThreadLabel(JavascriptThread thread) throws DebugException;

    /**
     * Label for stack frame to be shown in the Debug view.
     */
    String getStackFrameLabel(StackFrame stackFrame) throws DebugException;
  }

  /**
   * Performs breakpoint synchronization between remote VM and Eclipse IDE. This operation is
   * partially asynchronous: it blocks for reading breakpoints, but returns before all remote
   * changes are performed. When operations is fully complete, callback gets invoked.
   */
  void synchronizeBreakpoints(BreakpointSynchronizer.Direction direction,
      BreakpointSynchronizer.Callback callback);
}
