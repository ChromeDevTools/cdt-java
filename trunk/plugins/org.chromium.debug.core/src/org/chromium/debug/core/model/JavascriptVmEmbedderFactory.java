// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.JavascriptVmEmbedder.VmConnector;
import org.chromium.sdk.Browser;
import org.chromium.sdk.Browser.TabConnector;
import org.chromium.sdk.Browser.TabFetcher;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.StandaloneVm;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

public class JavascriptVmEmbedderFactory {
  public static JavascriptVmEmbedder.ConnectionToRemote connectToChromeDevTools(String host,
      int port, NamedConnectionLoggerFactory connectionLoggerFactory,
      final TabSelector tabSelector) throws CoreException {

    SocketAddress address = new InetSocketAddress(host, port);
    final Browser browser = browserCache.getOrCreateBrowser(address, connectionLoggerFactory);

    final TabFetcher tabFetcher;
    try {
      tabFetcher = browser.createTabFetcher();
    } catch (UnsupportedVersionException e) {
      throw newCoreException(e);
    } catch (IOException e) {
      throw newCoreException(e);
    }

    return new JavascriptVmEmbedder.ConnectionToRemote() {
      public JavascriptVmEmbedder.VmConnector selectVm() throws CoreException {
        Browser.TabConnector targetTabConnector;
        try {
          targetTabConnector = tabSelector.selectTab(tabFetcher);
        } catch (IOException e) {
          throw newCoreException("Failed to get tabs for debugging", e);
        }
        if (targetTabConnector == null) {
          return null;
        }

        return new EmbeddingTabConnector(targetTabConnector);
      }

      public void disposeConnection() {
        tabFetcher.dismiss();
      }
    };
  }

  private static final class EmbeddingTabConnector implements JavascriptVmEmbedder.VmConnector {
    private final Browser.TabConnector targetTabConnector;

    EmbeddingTabConnector(Browser.TabConnector targetTabConnector) {
      this.targetTabConnector = targetTabConnector;
    }

    public JavascriptVmEmbedder attach(final JavascriptVmEmbedder.Listener embedderListener,
        final DebugEventListener debugEventListener) throws CoreException {
      TabDebugEventListener tabDebugEventListener = new TabDebugEventListener() {
        public DebugEventListener getDebugEventListener() {
          return debugEventListener;
        }
        public void closed() {
          embedderListener.closed();
        }
        public void navigated(String newUrl) {
          embedderListener.reset();
        }
      };
      final BrowserTab browserTab;
      try {
        browserTab = targetTabConnector.attach(tabDebugEventListener);
      } catch (IOException e) {
        throw newCoreException("Failed to connect to browser tab", e);
      }
      return new JavascriptVmEmbedder() {
        public JavascriptVm getJavascriptVm() {
          return browserTab;
        }

        public String getTargetName() {
          return Messages.DebugTargetImpl_TargetName;
        }

        public String getThreadName() {
          return browserTab.getUrl();
        }
      };
    }
  }

  public static JavascriptVmEmbedder.ConnectionToRemote connectToStandalone(String host, int port,
      NamedConnectionLoggerFactory connectionLoggerFactory) {
    SocketAddress address = new InetSocketAddress(host, port);
    ConnectionLogger connectionLogger =
      connectionLoggerFactory.createLogger(address.toString());
    final StandaloneVm standaloneVm = BrowserFactory.getInstance().createStandalone(address,
        connectionLogger);

    return new JavascriptVmEmbedder.ConnectionToRemote() {
      public JavascriptVmEmbedder.VmConnector selectVm() {
        return new JavascriptVmEmbedder.VmConnector() {
          public JavascriptVmEmbedder attach(JavascriptVmEmbedder.Listener embedderListener,
              DebugEventListener debugEventListener)
              throws CoreException {
            embedderListener = null;
            try {
              standaloneVm.attach(debugEventListener);
            } catch (IOException e) {
              throw newCoreException("Failed to connect to Standalone V8 VM", e);
            } catch (UnsupportedVersionException e) {
              throw newCoreException("Failed to connect to Standalone V8 VM", e);
            }
            return new JavascriptVmEmbedder() {
              public JavascriptVm getJavascriptVm() {
                return standaloneVm;
              }
              public String getTargetName() {
                String embedderName = standaloneVm.getEmbedderName();
                String vmVersion = standaloneVm.getVmVersion();
                String disconnectReason = standaloneVm.getDisconnectReason();
                String targetTitle;
                if (embedderName == null) {
                  targetTitle = ""; //$NON-NLS-1$
                } else {
                  targetTitle = MessageFormat.format(
                      Messages.JavascriptVmEmbedderFactory_TargetName0, embedderName, vmVersion);
                }
                boolean isAttached = standaloneVm.isAttached();
                if (!isAttached) {
                  String disconnectMessage;
                  if (disconnectReason == null) {
                    disconnectMessage = Messages.JavascriptVmEmbedderFactory_Terminated;
                  } else {
                    disconnectMessage = MessageFormat.format(
                        Messages.JavascriptVmEmbedderFactory_TerminatedWithReason,
                        disconnectReason);
                  }
                  targetTitle = "<" + disconnectMessage + "> " + targetTitle;
                }
                return targetTitle;
              }
              public String getThreadName() {
                return ""; //$NON-NLS-1$
              }
            };
          }
        };
      }

      public void disposeConnection() {
        // Nothing to do. We do not take connection for ConnectionToRemote.
      }
    };
  }

  /**
   * A temporary tab selector interface for Wip protocol.
   * TODO: remove when we can get list of tabs from browser.
   */
  public interface WipTabSelector {
    TabConnector selectTab(WipBrowser browser);
  }

  public static JavascriptVmEmbedder.ConnectionToRemote connectToWipBrowser(String host,
      int port, final NamedConnectionLoggerFactory browserLoggerFactory,
      final NamedConnectionLoggerFactory tabLoggerFactory, final WipTabSelector tabSelector)
      throws CoreException {

    InetSocketAddress address = new InetSocketAddress(host, port);
    WipBrowserFactory.LoggerFactory factory = new WipBrowserFactory.LoggerFactory() {
      @Override
      public ConnectionLogger newBrowserConnectionLogger() {
        return browserLoggerFactory.createLogger("Connection to browser");
      }

      @Override
      public ConnectionLogger newTabConnectionLogger() {
        return browserLoggerFactory.createLogger("Connection to tab");
      }
    };
    final WipBrowser browser =
        WipBrowserFactory.INSTANCE.createBrowser(address, factory);

    return new JavascriptVmEmbedder.ConnectionToRemote() {
      @Override
      public VmConnector selectVm() throws CoreException {
        Browser.TabConnector targetTabConnector = tabSelector.selectTab(browser);
        if (targetTabConnector == null) {
          return null;
        }
        return new EmbeddingTabConnector(targetTabConnector);
      }

      @Override
      public void disposeConnection() {
      }
    };
  }

  private static CoreException newCoreException(String message, Throwable cause) {
    return new CoreException(
        new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID, message, cause));
  }
  private static CoreException newCoreException(Exception e) {
    return new CoreException(
        new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
            "Failed to connect to the remote browser", e));
  }

  private static final BrowserCache browserCache = new BrowserCache();
  /**
   * Cache of browser instances.
   */
  private static class BrowserCache {

    /**
     * Tries to return already created instance of Browser connected to {@code address}
     * or create new instance.
     * However, it creates a new instance each time that {@code ConnectionLogger} is not null
     * (because you cannot add connection logger to existing connection).
     * @throws CoreException if browser can't be created because of conflict with connectionLogger
     */
    synchronized Browser getOrCreateBrowser(final SocketAddress address,
        final NamedConnectionLoggerFactory connectionLoggerFactory) throws CoreException {
      Browser result = address2Browser.get(address);
      if (result == null) {

        ConnectionLogger.Factory wrappedFactory = new ConnectionLogger.Factory() {
          public ConnectionLogger newConnectionLogger() {
            return connectionLoggerFactory.createLogger(address.toString());
          }
        };
        result = createBrowserImpl(address, wrappedFactory);

        address2Browser.put(address, result);
      }
      return result;
    }
    private Browser createBrowserImpl(SocketAddress address,
        ConnectionLogger.Factory connectionLoggerFactory) {
      return BrowserFactory.getInstance().create(address, connectionLoggerFactory);
    }

    private final Map<SocketAddress, Browser> address2Browser =
        new HashMap<SocketAddress, Browser>();
  }
}
