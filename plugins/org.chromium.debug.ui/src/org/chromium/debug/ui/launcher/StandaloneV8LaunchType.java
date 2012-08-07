// Copyright 2009 Google Inc. All Rights Reserved.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.core.model.BreakpointSynchronizer;
import org.chromium.debug.core.model.JavascriptVmEmbedder;
import org.chromium.debug.core.model.JavascriptVmEmbedderFactory;
import org.chromium.debug.core.model.NamedConnectionLoggerFactory;
import org.chromium.sdk.ConnectionLogger;
import org.eclipse.debug.core.ILaunch;

public class StandaloneV8LaunchType extends LaunchTypeBase {
  @Override
  protected JavascriptVmEmbedder.ConnectionToRemote createConnectionToRemote(String host, int port,
      final ILaunch launch, boolean addConsoleLogger) {
    NamedConnectionLoggerFactory consoleFactory;
    if (addConsoleLogger) {
      consoleFactory = new NamedConnectionLoggerFactory() {
        public ConnectionLogger createLogger(String title) {
          return LaunchTypeBase.createConsoleAndLogger(launch, false, title);
        }
      };
    } else {
      consoleFactory = NO_CONNECTION_LOGGER_FACTORY;
    }
    return JavascriptVmEmbedderFactory.connectToStandalone(host, port, consoleFactory);
  }

  @Override
  protected BreakpointSynchronizer.Direction getPresetSyncDirection() {
    return null;
  }
}