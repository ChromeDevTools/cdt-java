// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.ConnectionLoggerFactory;
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.internal.core.LaunchConfiguration;

/**
 * A launch configuration delegate for the JavaScript debugging.
 */
public abstract class LaunchType implements ILaunchConfigurationDelegate {

  public static class Chromium extends LaunchType {
    @Override
    protected Attachable createAttachable(int port, ILaunch launch, boolean addConsoleLogger)
        throws CoreException {
      ConnectionLoggerFactory consoleFactory =
        addConsoleLogger ? CONNECTION_LOGGER_FACTORY : NO_CONNECTION_LOGGER_FACTORY;
      return JavascriptVmEmbedderFactory.connectToChromeDevTools(port, null,
          new DialogBasedTabSelector(), consoleFactory);
    }

    /**
     * This thing is responsible for creating a separate launch that holds
     * logger console pseudo-projects.
     * TODO(peter.rybin): these projects stay as zombies under the launch; fix it
     */
    private final static ConnectionLoggerFactory CONNECTION_LOGGER_FACTORY =
        new ConnectionLoggerFactory() {
      private final ILaunch commonLaunch;

      {
        // Let's create configuration first. We might need to create file that holds it.
        IPath configurationPath;
        try {
          configurationPath = initializeConfigurationFile();
        } catch (IOException e) {
          throw new RuntimeException("Failed to create configuration file", e); //$NON-NLS-1$
        }

        LaunchConfiguration configuration = new LaunchConfiguration(configurationPath) {
          @Override
          public boolean isLocal() {
            return true;
          }
        };
        commonLaunch = new Launch(configuration, ILaunchManager.DEBUG_MODE, null);
      }

      public ConnectionLogger createLogger(String title) {
        ConnectionLogger logger = LaunchType.createConsoleAndLogger(commonLaunch, title);
        // Active the launch (again if it already has been removed)
        DebugPlugin.getDefault().getLaunchManager().addLaunch(commonLaunch);
        return logger;
      }

      /**
       * Creates file so that configuration could be read from some location.
       */
      private IPath initializeConfigurationFile() throws IOException {
        IPath configurationPath = ChromiumDebugPlugin.getDefault().getStateLocation().append(
            Messages.LaunchType_LogConsoleLaunchName
            + "." + ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION); //$NON-NLS-1$
        String osPath = configurationPath.toOSString();
        File file = new File(osPath);
        synchronized (this) {
          if (!file.isFile()) {
            Writer writer = new OutputStreamWriter(new FileOutputStream(file));
            writer.write(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" //$NON-NLS-1$
                + "<launchConfiguration " //$NON-NLS-1$
                + "type=\"org.chromium.debug.ui.ConsolePseudoConfigurationType\"/>"); //$NON-NLS-1$
            writer.close();
          }
        }
        return configurationPath;
      }
    };
  }

  public static class StandaloneV8 extends LaunchType {
    @Override
    protected Attachable createAttachable(int port, final ILaunch launch,
        boolean addConsoleLogger) {
      ConnectionLoggerFactory consoleFactory;
      if (addConsoleLogger) {
        consoleFactory = new ConnectionLoggerFactory() {
          public ConnectionLogger createLogger(String title) {
            return LaunchType.createConsoleAndLogger(launch, title);
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

      boolean addNetworkConsole = config.getAttribute(LaunchType.ADD_NETWORK_CONSOLE, false);

      JavascriptVmEmbedder.Attachable attachable =
          createAttachable(port, launch, addNetworkConsole);

      String projectNameBase = config.getName();

      DebugTargetImpl target = new DebugTargetImpl(launch);
      try {
        boolean attached = target.attach(
            projectNameBase,
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

  protected abstract JavascriptVmEmbedder.Attachable createAttachable(int port, ILaunch launch,
      boolean addConsoleLogger) throws CoreException;

  private static void terminateTarget(DebugTargetImpl target) {
    target.setDisconnected(true);
    target.fireTerminateEvent();
  }

  public static ConnectionLogger createConsoleAndLogger(ILaunch launch, String title) {
    ConsolePseudoProcess.WritableStreamMonitor consolePart =
        new ConsolePseudoProcess.WritableStreamMonitor();
    ConnectionLoggerImpl logger = new ConnectionLoggerImpl(consolePart, consolePart);

    ConsolePseudoProcess consolePseudoProcess = new ConsolePseudoProcess(launch, title, consolePart,
        logger.getConnectionTerminate());

    consolePart.startFlushing();
    return logger;
  }

  private static final ConnectionLoggerFactory NO_CONNECTION_LOGGER_FACTORY =
      new ConnectionLoggerFactory() {
    public ConnectionLogger createLogger(String title) {
      return null;
    }
  };
}