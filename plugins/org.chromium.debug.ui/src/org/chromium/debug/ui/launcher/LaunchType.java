// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.chromium.debug.core.model.ConnectionLoggerImpl;
import org.chromium.debug.core.model.ConsolePseudoProcess;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.Destructable;
import org.chromium.debug.core.model.DestructingGuard;
import org.chromium.debug.core.model.JavascriptVmEmbedder;
import org.chromium.debug.core.model.JavascriptVmEmbedderFactory;
import org.chromium.debug.core.model.NamedConnectionLoggerFactory;
import org.chromium.debug.ui.ChromiumDebugUIPlugin;
import org.chromium.debug.ui.DialogBasedTabSelector;
import org.chromium.debug.ui.PluginUtil;
import org.chromium.sdk.ConnectionLogger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * A launch configuration delegate for the JavaScript debugging.
 */
public abstract class LaunchType implements ILaunchConfigurationDelegate {

  // TODO(peter.rybin): move this class out into a package
  public static class Chromium extends LaunchType {
    @Override
    protected JavascriptVmEmbedder.ConnectionToRemote createConnectionToRemote(int port,
        ILaunch launch, boolean addConsoleLogger) throws CoreException {
      NamedConnectionLoggerFactory consoleFactory =
          addConsoleLogger ? getLoggerFactory() : NO_CONNECTION_LOGGER_FACTORY;
      return JavascriptVmEmbedderFactory.connectToChromeDevTools(port, consoleFactory,
          new DialogBasedTabSelector());
    }

    private static NamedConnectionLoggerFactory loggerFactory = null;

    private synchronized static NamedConnectionLoggerFactory getLoggerFactory()
        throws CoreException {
      if (loggerFactory == null) {
        loggerFactory = new ConnectionLoggerFactoryImpl();
      }
      return loggerFactory;
    }

    /**
     * This thing is responsible for creating a separate launch that holds
     * logger console pseudo-projects.
     * TODO(peter.rybin): these projects stay as zombies under the launch; fix it
     */
    private static class ConnectionLoggerFactoryImpl implements NamedConnectionLoggerFactory {
      private final ILaunch commonLaunch;

      /**
       * Here we create launch configuration and launch, which will hold console pseudo-process.
       * This is a bit messy, because ILaunchManager mostly supports user creation of new
       * configurations in UI.
       */
      public ConnectionLoggerFactoryImpl() throws CoreException {
        String location = Messages.LaunchType_LogConsoleLaunchName + "." + //$NON-NLS-1$
            ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION;
        String memento =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + //$NON-NLS-1$
            "<launchConfiguration " + //$NON-NLS-1$
            "local=\"true\" path=\"" + location + "\"/>"; //$NON-NLS-1$ //$NON-NLS-2$

        ILaunchConfiguration configuration =
            DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(memento);
        try {
          initializeConfigurationFile(configuration.getLocation());
        } catch (IOException e) {
          throw new CoreException(new Status(IStatus.ERROR, ChromiumDebugUIPlugin.PLUGIN_ID,
              "Failed to create launch configuration file", e)); //$NON-NLS-1$
        }
        commonLaunch = new Launch(configuration, ILaunchManager.DEBUG_MODE, null);
      }

      public ConnectionLogger createLogger(String title) {
        return LaunchType.createConsoleAndLogger(commonLaunch, true, title);
      }

      /**
       * Creates file so that configuration could be read from some location.
       */
      private void initializeConfigurationFile(IPath configurationPath) throws IOException {
        String osPath = configurationPath.toOSString();
        File file = new File(osPath);
        synchronized (this) {
          Writer writer = new OutputStreamWriter(new FileOutputStream(file));
          writer.write(
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + //$NON-NLS-1$
              "<launchConfiguration " + //$NON-NLS-1$
              "type=\"org.chromium.debug.ui.ConsolePseudoConfigurationType\"/>"); //$NON-NLS-1$
          writer.close();
        }
      }
    }
  }

  public static class StandaloneV8 extends LaunchType {
    @Override
    protected JavascriptVmEmbedder.ConnectionToRemote createConnectionToRemote(int port,
        final ILaunch launch, boolean addConsoleLogger) {
      NamedConnectionLoggerFactory consoleFactory;
      if (addConsoleLogger) {
        consoleFactory = new NamedConnectionLoggerFactory() {
          public ConnectionLogger createLogger(String title) {
            return LaunchType.createConsoleAndLogger(launch, false, title);
          }
        };
      } else {
        consoleFactory = NO_CONNECTION_LOGGER_FACTORY;
      }
      return JavascriptVmEmbedderFactory.connectToStandalone(port, consoleFactory);
    }
  }

  /** Launch configuration attribute (debug port). */
  public static final String CHROMIUM_DEBUG_PORT = "debug_port"; //$NON-NLS-1$

  public static final String ADD_NETWORK_CONSOLE = "add_network_console"; //$NON-NLS-1$

  public void launch(ILaunchConfiguration config, String mode, final ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    // Chromium JavaScript launch is only supported for debugging.
    if (mode.equals(ILaunchManager.DEBUG_MODE)) {
      int port =
          config.getAttribute(LaunchType.CHROMIUM_DEBUG_PORT,
              PluginVariablesUtil.getValueAsInt(PluginVariablesUtil.DEFAULT_PORT));

      boolean addNetworkConsole = config.getAttribute(LaunchType.ADD_NETWORK_CONSOLE, false);

      JavascriptVmEmbedder.ConnectionToRemote remoteServer =
          createConnectionToRemote(port, launch, addNetworkConsole);
      try {

        String projectNameBase = config.getName();

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

          final DebugTargetImpl target = new DebugTargetImpl(launch);

          Destructable targetDestructor = new Destructable() {
            public void destruct() {
              terminateTarget(target);
            }
          };
          destructingGuard.addValue(targetDestructor);
          boolean attached = target.attach(
              projectNameBase, remoteServer, destructingGuard,
              new Runnable() {
                public void run() {
                  PluginUtil.openProjectExplorerView();
                }
              },
              monitor);
          if (!attached) {
            // Error
            return;
          }

          launch.setSourceLocator(target.getSourceLocator());

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
  }

  protected abstract JavascriptVmEmbedder.ConnectionToRemote createConnectionToRemote(int port,
      ILaunch launch, boolean addConsoleLogger) throws CoreException;

  private static void terminateTarget(DebugTargetImpl target) {
    target.setDisconnected(true);
    target.fireTerminateEvent();
  }

  public static ConnectionLogger createConsoleAndLogger(final ILaunch launch,
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
          // Active the launch (again if it already has been removed)
          DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
       }
      }
    };

    return new ConnectionLoggerImpl(consoleRetransmitter, consoleController);
  }

  private static final NamedConnectionLoggerFactory NO_CONNECTION_LOGGER_FACTORY =
      new NamedConnectionLoggerFactory() {
    public ConnectionLogger createLogger(String title) {
      return null;
    }
  };
}