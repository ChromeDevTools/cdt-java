// Copyright 2009 Google Inc. All Rights Reserved.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.core.model.JavascriptVmEmbedder;
import org.chromium.debug.core.model.JavascriptVmEmbedderFactory;
import org.chromium.debug.core.model.NamedConnectionLoggerFactory;
import org.chromium.debug.ui.ChromiumDebugUIPlugin;
import org.chromium.debug.ui.DialogBasedTabSelector;
import org.chromium.sdk.ConnectionLogger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ChromiumLaunchType extends LaunchTypeBase {
  @Override
  protected JavascriptVmEmbedder.ConnectionToRemote createConnectionToRemote(int port,
      ILaunch launch, boolean addConsoleLogger) throws CoreException {
    NamedConnectionLoggerFactory consoleFactory =
        addConsoleLogger ? getLoggerFactory() : NO_CONNECTION_LOGGER_FACTORY;
    return JavascriptVmEmbedderFactory.connectToChromeDevTools(port, consoleFactory,
        new DialogBasedTabSelector());
  }

  private static NamedConnectionLoggerFactory loggerFactory = null;

  private static synchronized NamedConnectionLoggerFactory getLoggerFactory()
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
    private static final String LAUNCH_CONFIGURATION_FILE_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + //$NON-NLS-1$
                "<launchConfiguration " + //$NON-NLS-1$
                "type=\"org.chromium.debug.ui.ConsolePseudoConfigurationType\"/>";

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
      return LaunchTypeBase.createConsoleAndLogger(commonLaunch, true, title);
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
            LAUNCH_CONFIGURATION_FILE_CONTENT); //$NON-NLS-1$
        writer.close();
      }
    }
  }
}