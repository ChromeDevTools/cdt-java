// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.io.Writer;

import org.chromium.debug.core.model.ConnectionLoggerImpl;
import org.chromium.debug.core.model.ConsolePseudoProcess;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.JavascriptVmEmbedder;
import org.chromium.debug.core.model.JavascriptVmEmbedderFactory;
import org.chromium.debug.core.model.JavascriptVmEmbedder.Attachable;
import org.chromium.debug.ui.DialogBasedTabSelector;
import org.chromium.debug.ui.PluginUtil;
import org.chromium.sdk.ConnectionLogger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * A launch configuration delegate for the JavaScript debugging.
 */
public abstract class LaunchType implements ILaunchConfigurationDelegate {

  public static class Chromium extends LaunchType {
    @Override
    protected Attachable createAttachable(int port, ConnectionLogger logger) throws CoreException {
      return JavascriptVmEmbedderFactory.connectToChromeDevTools(port, logger,
          new DialogBasedTabSelector());
    }
  }

  public static class StandaloneV8 extends LaunchType {
    @Override
    protected Attachable createAttachable(int port, ConnectionLogger logger) {
      return JavascriptVmEmbedderFactory.connectToStandalone(port, logger);
    }
  }

  /** Launch configuration attribute (debug port). */
  public static final String CHROMIUM_DEBUG_PORT = "debug_port"; //$NON-NLS-1$

  /** Launch configuration attribute (target project name). */
  public static final String CHROMIUM_DEBUG_PROJECT_NAME = "debug_project_name"; //$NON-NLS-1$

  public static final String ADD_NETWORK_CONSOLE = "add_network_console"; //$NON-NLS-1$

  public void launch(ILaunchConfiguration config, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    // Chromium JavaScript launch is only supported for debugging.
    if (mode.equals(ILaunchManager.DEBUG_MODE)) {
      int port =
          config.getAttribute(LaunchType.CHROMIUM_DEBUG_PORT,
              PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT));
      String projectName =
          config.getAttribute(LaunchType.CHROMIUM_DEBUG_PROJECT_NAME,
              PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_PROJECT_NAME));


      boolean addNetworkConsole = config.getAttribute(LaunchType.ADD_NETWORK_CONSOLE, false);

      // This is some important part of console, which we construct first.
      ConsolePseudoProcess.WritableStreamMonitor consolePart;
      ConnectionLogger logger;
      if (addNetworkConsole) {
        consolePart = new ConsolePseudoProcess.WritableStreamMonitor();
        Writer writer = consolePart;
        logger = new ConnectionLoggerImpl(writer);
      } else {
        consolePart = null;
        logger = null;
      }

      JavascriptVmEmbedder.Attachable attachable = createAttachable(port, logger);

      // Construct process after we have constructed Attachable (and know it is constructed OK).
      ConsolePseudoProcess consolePseudoProcess;
      if (consolePart == null) {
        consolePseudoProcess = null;
      } else {
        consolePseudoProcess = new ConsolePseudoProcess(launch,
            Messages.LaunchType_DebuggerChromeConnection, consolePart);
        // Framework should have connected to output already.
        consolePart.startFlushing();
      }

      DebugTargetImpl target = new DebugTargetImpl(launch, consolePseudoProcess);
      try {
        boolean attached = target.attach(
            projectName,
            attachable,
            new Runnable() {
              public void run() {
                PluginUtil.openProjectExplorerView();
              }
            },
            monitor);
        if (!attached) {
          terminateTarget(target);
        }
      } catch (CoreException e) {
        terminateTarget(target);
        throw e;
      } catch (RuntimeException e) {
        terminateTarget(target);
        throw e;
      } finally {
        launch.addDebugTarget(target);
        monitor.done();
      }
    }
  }

  protected abstract JavascriptVmEmbedder.Attachable createAttachable(int port, ConnectionLogger logger) throws CoreException;

  private static void terminateTarget(DebugTargetImpl target) {
    target.setDisconnected(true);
    target.fireTerminateEvent();
  }
}