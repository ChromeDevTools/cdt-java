// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.ToolName;
import org.chromium.debug.core.tools.devtools.DevToolsServiceHandler;
import org.chromium.debug.core.tools.devtools.DevToolsServiceHandler.TabIdAndUrl;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.chromium.debug.core.tools.v8.model.mirror.Execution;
import org.chromium.debug.core.transport.SocketConnection;
import org.chromium.debug.core.util.WorkspaceUtil;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * An IDebugTarget implementation for remote Chromium Javascript debugging.
 */
public class DebugTargetImpl extends DebugElementImpl implements IDebugTarget {

  private static final IThread[] EMPTY_THREADS = new IThread[0];

  private final ILaunch launch;

  private final SocketConnection socketConnection;

  private final V8DebuggerToolHandler v8DebuggerHandler;

  private final DevToolsServiceHandler devToolsServiceHandler;

  private final Execution execution;

  private volatile boolean isSuspended = false;

  private volatile boolean isDisconnected = false;

  private final IThread[] threads;

  private IProject debugProject;

  public DebugTargetImpl(ILaunch launch, IProcess process, String host,
      int port, boolean breakOnStartup, String projectName)
      throws CoreException {
    super(null);
    this.execution = new Execution(this);
    this.socketConnection = ChromiumDebugPlugin.getDefault().getSocketConnection(host, port);
    if (socketConnection.isConnected()) {
      throw newCoreException(Messages.DebugTargetImpl_CannotStartMultipleDebuggers, null);
    }
    this.launch = launch;
    this.v8DebuggerHandler = new V8DebuggerToolHandler(execution);
    this.devToolsServiceHandler = new DevToolsServiceHandler(this);
    this.getSocketConnection().setToolHandler(ToolName.V8_DEBUGGER, this.v8DebuggerHandler);
    this.getSocketConnection().setToolHandler(
        ToolName.DEVTOOLS_SERVICE, this.devToolsServiceHandler);
    this.threads = new IThread[] { v8DebuggerHandler.getThread() };
    try {
      getSocketConnection().startup();
    } catch (IOException e) {
      throw newCoreException(
          Messages.DebugTargetImpl_FailedToStartSocketConnection, e);
    }
    int targetTab = selectTargetTab();
    if (targetTab != -1) {
      this.debugProject = WorkspaceUtil.createEmptyProject(projectName);
      v8DebuggerHandler.attachToTab(targetTab);
      WorkspaceUtil.openProjectExplorerView();
    } else {
      ChromiumDebugPlugin.shutdownConnection(false);
    }
  }

  private CoreException newCoreException(String message, Exception e) {
    return new CoreException(
        new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID, message, e));
  }

  private int selectTargetTab() {
    final int[] result = new int[1];
    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final List<String[]> urlAndIdPairs =
            Collections.synchronizedList(new ArrayList<String[]>());
        final ChromiumTabSelectionDialog dialog =
            new ChromiumTabSelectionDialog(shell, urlAndIdPairs);
        dialog.setBlockOnOpen(true);
        devToolsServiceHandler.listTabs(
            new DevToolsServiceHandler.ListTabsHandler() {
              @Override
              public void tabsReceived(List<TabIdAndUrl> tabs) {
                for (TabIdAndUrl pair : tabs) {
                  urlAndIdPairs.add(new String[] { pair.url, String.valueOf(pair.id) });
                }
                dialog.setDataReady();
              }
            });
        int dialogResult = dialog.open();
        if (dialogResult == ChromiumTabSelectionDialog.OK) {
          result[0] = dialog.getSelectedId();
        } else {
          result[0] = -1;
        }
      }
    });
    return result[0];
  }

  @Override
  public String getName() throws DebugException {
    return Messages.DebugTargetImpl_TargetName;
  }

  @Override
  public IProcess getProcess() {
    return null;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    return isDisconnected() ? EMPTY_THREADS : threads;
  }

  @Override
  public boolean hasThreads() throws DebugException {
    return getThreads().length > 0;
  }

  @Override
  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return ChromiumDebugPlugin.DEBUG_MODEL_ID.equals(breakpoint.getModelIdentifier()) &&
        !isDisconnected();
  }

  @Override
  public DebugTargetImpl getDebugTarget() {
    return this;
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public String getModelIdentifier() {
    return ChromiumDebugPlugin.DEBUG_MODEL_ID;
  }

  @Override
  public boolean canTerminate() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public void terminate() throws DebugException {
  }

  @Override
  public boolean canResume() {
    return !isDisconnected() && isSuspended();
  }

  @Override
  public boolean canSuspend() {
    // Immediate thread suspend is not supported by V8 (does not make sense)
    return false;
  }

  public boolean isSuspended() {
    return isSuspended;
  }

  public void suspended(int detail) {
    isSuspended = true;
    fireSuspendEvent(detail);
  }

  @Override
  public void resume() throws DebugException {
    try {
      v8DebuggerHandler.resumeRequested();
      // Let's pretend Chromium does respond
      // to the "continue" request immediately
      resumed(DebugEvent.CLIENT_REQUEST);
    } catch (IOException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  public void resumed(int detail) {
    fireResumeEvent(detail);
  }

  @Override
  public void suspend() throws DebugException {
    // Immediate thread suspend is not supported by V8 (does not make sense)
  }

  @Override
  public boolean canDisconnect() {
    return !isDisconnected();
  }

  @Override
  public void disconnect() throws DebugException {
    if (v8DebuggerHandler.isAttached()) {
      v8DebuggerHandler.detachFromTab();
    }
    ChromiumDebugPlugin.shutdownConnection(true);
    fireTerminateEvent();
  }

  @Override
  public boolean isDisconnected() {
    return isDisconnected;
  }

  @Override
  public IMemoryBlock getMemoryBlock(long startAddress, long length)
      throws DebugException {
    return null;
  }

  @Override
  public boolean supportsStorageRetrieval() {
    return false;
  }

  public SocketConnection getSocketConnection() {
    return socketConnection;
  }

  public IProject getDebugProject() {
    return debugProject;
  }

  /**
   * Fires a debug event
   *
   * @param event
   *          the event to be fired
   */
  public void fireEvent(DebugEvent event) {
    DebugPlugin debugPlugin = DebugPlugin.getDefault();
    if (debugPlugin != null) {
      debugPlugin.fireDebugEventSet(new DebugEvent[] { event });
    }
  }

  public void fireEventForThread(int kind, int detail) {
    try {
      IThread[] threads = getThreads();
      if (threads.length > 0) {
        fireEvent(new DebugEvent(threads[0], kind, detail));
      }
    } catch (DebugException e) {
      // Actually, this is not thrown in out getThreads()
      return;
    }
    if (threads.length == 0) {
      return;
    }
  }

  public void fireCreationEvent() {
    isDisconnected = false;
    fireEventForThread(DebugEvent.CREATE, DebugEvent.UNSPECIFIED);
  }

  public void fireResumeEvent(int detail) {
    isSuspended = false;
    fireEventForThread(DebugEvent.RESUME, detail);
    fireEvent(new DebugEvent(this, DebugEvent.RESUME, detail));
  }

  public void fireSuspendEvent(int detail) {
    isSuspended = true;
    fireEventForThread(DebugEvent.SUSPEND, detail);
    fireEvent(new DebugEvent(this, DebugEvent.SUSPEND, detail));
  }

  public void fireTerminateEvent() {
    fireEventForThread(DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED);
    fireEvent(new DebugEvent(
        this, DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED));
    fireEvent(new DebugEvent(getLaunch(), DebugEvent.TERMINATE,
        DebugEvent.UNSPECIFIED));
    isDisconnected = true;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    // handled by BreakpointProcessor
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    // handled by BreakpointProcessor
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    // handled by BreakpointProcessor
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter) {
    if (ILaunch.class.equals(adapter)) {
      return this.launch;
    }
    return super.getAdapter(adapter);
  }

  @Override
  public V8DebuggerToolHandler getHandler() {
    return v8DebuggerHandler;
  }

  public Execution getExecution() {
    return execution;
  }

}
