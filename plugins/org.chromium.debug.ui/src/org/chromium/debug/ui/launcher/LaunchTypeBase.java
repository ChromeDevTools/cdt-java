// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.util.concurrent.atomic.AtomicBoolean;

import org.chromium.debug.core.model.ConnectionLoggerImpl;
import org.chromium.debug.core.model.ConsolePseudoProcess;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.Destructable;
import org.chromium.debug.core.model.DestructingGuard;
import org.chromium.debug.core.model.JavascriptVmEmbedder;
import org.chromium.debug.core.model.NamedConnectionLoggerFactory;
import org.chromium.debug.core.model.VProjectWorkspaceBridge;
import org.chromium.debug.core.model.WorkspaceBridge;
import org.chromium.debug.ui.PluginUtil;
import org.chromium.sdk.ConnectionLogger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * A launch configuration delegate for the JavaScript debugging.
 */
public abstract class LaunchTypeBase implements ILaunchConfigurationDelegate {

  /** Launch configuration attribute (debug port). */
  public static final String CHROMIUM_DEBUG_PORT = "debug_port"; //$NON-NLS-1$

  public static final String ADD_NETWORK_CONSOLE = "add_network_console"; //$NON-NLS-1$

  public void launch(ILaunchConfiguration config, String mode, final ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    if (!mode.equals(ILaunchManager.DEBUG_MODE)) {
      // Chromium JavaScript launch is only supported for debugging.
      return;
    }

    int port =
        config.getAttribute(LaunchTypeBase.CHROMIUM_DEBUG_PORT,
            PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT));

    boolean addNetworkConsole = config.getAttribute(LaunchTypeBase.ADD_NETWORK_CONSOLE, false);

    JavascriptVmEmbedder.ConnectionToRemote remoteServer =
        createConnectionToRemote(port, launch, addNetworkConsole);
    try {

      final String projectNameBase = config.getName();

      DestructingGuard destructingGuard = new DestructingGuard();
      try {
        Destructable lauchDestructor = new Destructable() {
          public void destruct() {
            if (!launch.hasChildren()) {
              DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
            }
          }
        };

        destructingGuard.addValue(lauchDestructor);

        WorkspaceBridge.Factory bridgeFactory =
            new VProjectWorkspaceBridge.FactoryImpl(projectNameBase);

        final DebugTargetImpl target = new DebugTargetImpl(launch, bridgeFactory);

        Destructable targetDestructor = new Destructable() {
          public void destruct() {
            terminateTarget(target);
          }
        };
        destructingGuard.addValue(targetDestructor);

        boolean attached = target.attach(remoteServer, destructingGuard,
            OPENING_VIEW_ATTACH_CALLBACK, monitor);
        if (!attached) {
          // Error
          return;
        }

        launch.addDebugTarget(target);
        monitor.done();

        // All OK
        destructingGuard.discharge();
      } finally {
        destructingGuard.doFinally();
      }

    } finally {
      remoteServer.disposeConnection();
    }
  }

  protected abstract JavascriptVmEmbedder.ConnectionToRemote createConnectionToRemote(int port,
      ILaunch launch, boolean addConsoleLogger) throws CoreException;

  private static void terminateTarget(DebugTargetImpl target) {
    target.setDisconnected(true);
    target.fireTerminateEvent();
  }

  static ConnectionLogger createConsoleAndLogger(final ILaunch launch,
      final boolean addLaunchToManager, final String title) {
    final ConsolePseudoProcess.Retransmitter consoleRetransmitter =
        new ConsolePseudoProcess.Retransmitter();

    // This controller is responsible for creating ConsolePseudoProcess only on
    // logStarted call. Before this ConnectionLoggerImpl with all it fields should stay
    // garbage-collectible, because connection may not even start.
    ConnectionLoggerImpl.LogLifecycleListener consoleController =
        new ConnectionLoggerImpl.LogLifecycleListener() {
      private final AtomicBoolean alreadyStarted = new AtomicBoolean(false);

      public void logClosed() {
        consoleRetransmitter.processClosed();
      }

      public void logStarted(ConnectionLoggerImpl connectionLogger) {
        boolean res = alreadyStarted.compareAndSet(false, true);
        if (!res) {
          throw new IllegalStateException();
        }
        ConsolePseudoProcess consolePseudoProcess = new ConsolePseudoProcess(launch, title,
            consoleRetransmitter, connectionLogger.getConnectionTerminate());
        consoleRetransmitter.startFlushing();
        if (addLaunchToManager) {
          // Active the launch (again if it has already been removed)
          DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
       }
      }
    };

    return new ConnectionLoggerImpl(consoleRetransmitter, consoleController);
  }

  static final NamedConnectionLoggerFactory NO_CONNECTION_LOGGER_FACTORY =
      new NamedConnectionLoggerFactory() {
    public ConnectionLogger createLogger(String title) {
      return null;
    }
  };

  private static final Runnable OPENING_VIEW_ATTACH_CALLBACK = new Runnable() {
    public void run() {
      PluginUtil.openProjectExplorerView();
    }
  };
}