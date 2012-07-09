// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import org.chromium.debug.core.model.JavascriptVmEmbedder.ConnectionToRemote;
import org.chromium.debug.core.model.JavascriptVmEmbedderFactory;
import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.NamedConnectionLoggerFactory;
import org.chromium.debug.ui.DialogBasedTabSelector;
import org.chromium.sdk.ConnectionLogger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import org.chromium.sdk.wip.WipBackend;
import org.chromium.sdk.wip.eclipse.BackendRegistry;

public class WipLaunchType extends LaunchTypeBase {
  @Override
  protected ConnectionToRemote createConnectionToRemote(String host, int port,
      final ILaunch launch, boolean addConsoleLogger) throws CoreException {

    ILaunchConfiguration config = launch.getLaunchConfiguration();
    String wipBackendId = config.getAttribute(LaunchParams.WIP_BACKEND_ID, (String) null);

    if (wipBackendId == null) {
      throw new RuntimeException("Missing 'wip backend' parameter in launch config");
    }

    WipBackend backend;
    findWipBackend: {
      for (WipBackend nextBackend : BackendRegistry.INSTANCE.getBackends()) {
        if (nextBackend.getId().equals(wipBackendId)) {
          backend = nextBackend;
          break findWipBackend;
        }
      }
      // Nothing found.
      throw new RuntimeException("Cannot find required wip backend in Eclipse: " + wipBackendId);
    }

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

    return JavascriptVmEmbedderFactory.connectToWipBrowser(host, port, backend, consoleFactory,
        consoleFactory, DialogBasedTabSelector.WIP_INSTANCE);
  }
}
