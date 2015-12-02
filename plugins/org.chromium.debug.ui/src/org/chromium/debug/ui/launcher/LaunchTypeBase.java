// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.BreakpointSynchronizer.Direction;
import org.chromium.debug.core.model.ConnectionLoggerImpl;
import org.chromium.debug.core.model.ConsolePseudoProcess;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.IPredefinedSourceWrapProvider;
import org.chromium.debug.core.model.JavascriptVmEmbedder;
import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.NamedConnectionLoggerFactory;
import org.chromium.debug.core.model.SourceWrapSupport;
import org.chromium.debug.core.model.VProjectWorkspaceBridge;
import org.chromium.debug.core.model.WorkspaceBridge;
import org.chromium.debug.ui.PluginUtil;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.util.Destructable;
import org.chromium.sdk.util.DestructingGuard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * A launch configuration delegate for the JavaScript debugging.
 */
public abstract class LaunchTypeBase implements ILaunchConfigurationDelegate {

  public void launch(ILaunchConfiguration config, String mode, final ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    if (!mode.equals(ILaunchManager.DEBUG_MODE)) {
      // Chromium JavaScript launch is only supported for debugging.
      return;
    }


    String host = config.getAttribute(LaunchParams.CHROMIUM_DEBUG_HOST,
        PluginVariablesUtil.getValue(PluginVariablesUtil.DEFAULT_HOST));

    int port = config.getAttribute(LaunchParams.CHROMIUM_DEBUG_PORT,
        PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT));

    if (host == null && port == -1) {
      throw new RuntimeException("Missing parameters in launch config");
    }

    boolean addNetworkConsole = config.getAttribute(LaunchParams.ADD_NETWORK_CONSOLE, false);

    SourceWrapSupport sourceWrapSupport = createSourceWrapSupportFromConfig(config);

    JavascriptVmEmbedder.ConnectionToRemote remoteServer =
        createConnectionToRemote(host, port, launch, addNetworkConsole);
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

        final DebugTargetImpl target =
            new DebugTargetImpl(launch, bridgeFactory, sourceWrapSupport, getPresetSyncDirection());

        Destructable targetDestructor = new Destructable() {
          public void destruct() {
            terminateTarget(target);
          }
        };
        destructingGuard.addValue(targetDestructor);

        launch.addDebugTarget(target);

        boolean attached = DebugTargetImpl.attach(target, remoteServer, destructingGuard,
            OPENING_VIEW_ATTACH_CALLBACK, monitor);
        if (!attached) {
          // Cancel pressed.
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

  protected abstract JavascriptVmEmbedder.ConnectionToRemote createConnectionToRemote(String host,
      int port, ILaunch launch, boolean addConsoleLogger) throws CoreException;

  protected abstract Direction getPresetSyncDirection();

  private static void terminateTarget(DebugTargetImpl target) {
    try {
      target.terminate();
    } catch (DebugException e) {
      ChromiumDebugPlugin.log(e);
    }
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

  private static SourceWrapSupport createSourceWrapSupportFromConfig(ILaunchConfiguration config)
      throws CoreException {
    List<IPredefinedSourceWrapProvider.Entry> entries =
        LaunchParams.PredefinedSourceWrapperIds.resolveEntries(config);
    List<SourceWrapSupport.Wrapper> wrappers =
        new ArrayList<SourceWrapSupport.Wrapper>(entries.size());
    for (IPredefinedSourceWrapProvider.Entry en : entries) {
      wrappers.add(en.getWrapper());
    }
    return new SourceWrapSupport(wrappers);
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