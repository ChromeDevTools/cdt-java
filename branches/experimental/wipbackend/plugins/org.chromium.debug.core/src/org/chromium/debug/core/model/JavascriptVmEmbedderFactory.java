// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.join;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.ScriptNameManipulator;
import org.chromium.debug.core.util.JavaScriptRegExpSupport;
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
import org.chromium.sdk.wip.WipBackend;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowser.WipTabConnector;
import org.chromium.sdk.wip.WipBrowserFactory;
import org.chromium.sdk.wip.WipBrowserTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

public class JavascriptVmEmbedderFactory {
  public static JavascriptVmEmbedder.ConnectionToRemote connectToChromeDevTools(String host,
      int port, NamedConnectionLoggerFactory connectionLoggerFactory,
      final TabSelector tabSelector) throws CoreException {

    SocketAddress address = new InetSocketAddress(host, port);
    final Browser browser = browserCache.getOrCreateBrowser(address, connectionLoggerFactory);

    return connect(browser, tabSelector);
  }

  public static JavascriptVmEmbedder.ConnectionToRemote connectToWipBrowser(String host, int port,
      WipBackend backend,
      final NamedConnectionLoggerFactory browserLoggerFactory,
      final NamedConnectionLoggerFactory tabLoggerFactory,
      WipTabSelector tabSelector) throws CoreException {

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

    return connectWip(browser, backend, tabSelector);
  }

  private static JavascriptVmEmbedder.ConnectionToRemote connect(Browser browser,
      final TabSelector tabSelector) throws CoreException {

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

        return new EmbeddingTabConnectorImpl(targetTabConnector);
      }

      public void disposeConnection() {
        tabFetcher.dismiss();
      }
    };
  }

  private static JavascriptVmEmbedder.ConnectionToRemote connectWip(final WipBrowser browser,
      final WipBackend backend, final WipTabSelector tabSelector) throws CoreException {
    return new JavascriptVmEmbedder.ConnectionToRemote() {
      public JavascriptVmEmbedder.VmConnector selectVm() throws CoreException {
        WipTabSelector.BrowserAndBackend browserAndBackend = new WipTabSelector.BrowserAndBackend() {
          @Override public WipBrowser getBrowser() {
            return browser;
          }
          @Override public WipBackend getBackend() {
            return backend;
          }
        };
        WipBrowser.WipTabConnector targetTabConnector;
        try {
          targetTabConnector = tabSelector.selectTab(browserAndBackend);
        } catch (IOException e) {
          throw newCoreException("Failed to get tabs for debugging", e);
        }
        if (targetTabConnector == null) {
          return null;
        }

        return new WipEmbeddingTabConnector(targetTabConnector, backend.getId());
      }

      public void disposeConnection() {
      }
    };
  }

  private static abstract class EmbeddingTabConnectorBase<TC>
      implements JavascriptVmEmbedder.VmConnector {
    private final TC targetTabConnector;

    EmbeddingTabConnectorBase(TC targetTabConnector) {
      this.targetTabConnector = targetTabConnector;
    }

    protected TC getTabConnector() {
      return targetTabConnector;
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

      return attach(tabDebugEventListener);
    }

    protected abstract JavascriptVmEmbedder attach(TabDebugEventListener tabDebugEventListener)
        throws CoreException;

    protected static abstract class EmbedderBase implements JavascriptVmEmbedder {
      @Override
      public ScriptNameManipulator getScriptNameManipulator() {
        return BROWSER_SCRIPT_NAME_MANIPULATOR;
      }
    }

    private static final ScriptNameManipulator BROWSER_SCRIPT_NAME_MANIPULATOR =
        new ScriptNameManipulator() {
      @Override
      public FilePath getFileName(String scriptName) {
        String filePath;
        try {
          URI uri = new URI(scriptName);
          filePath = uri.getPath();
        } catch (URISyntaxException e) {
          filePath = scriptName;
        }
        return new StringBasedFileName(filePath);
      }

      @Override
      public ScriptNamePattern createPattern(List<String> components) {
        String pathString = join(components, "/");
        return new ScriptNamePattern(
            JavaScriptRegExpSupport.encodeLiteral(pathString) + "/?($|\\?)");
      }
    };
  }

  private static class EmbeddingTabConnectorImpl
      extends EmbeddingTabConnectorBase<Browser.TabConnector> {
    EmbeddingTabConnectorImpl(TabConnector targetTabConnector) {
      super(targetTabConnector);
    }

    @Override
    protected JavascriptVmEmbedder attach(TabDebugEventListener tabDebugEventListener)
        throws CoreException {
      final BrowserTab browserTab;
      try {
        browserTab = getTabConnector().attach(tabDebugEventListener);
      } catch (IOException e) {
        throw newCoreException("Failed to connect to browser tab", e);
      }
      return new EmbedderBase() {
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

  private static class WipEmbeddingTabConnector
      extends EmbeddingTabConnectorBase<WipBrowser.WipTabConnector> {
    private final String backendId;

    WipEmbeddingTabConnector(WipTabConnector targetTabConnector, String backendId) {
      super(targetTabConnector);
      this.backendId = backendId;
    }

    @Override
    protected JavascriptVmEmbedder attach(TabDebugEventListener tabDebugEventListener)
        throws CoreException {
      final WipBrowserTab browserTab;
      try {
        browserTab = getTabConnector().attach(tabDebugEventListener);
      } catch (IOException e) {
        throw newCoreException("Failed to connect to browser tab", e);
      }
      return new EmbedderBase() {
        public JavascriptVm getJavascriptVm() {
          return browserTab.getJavascriptVm();
        }

        public String getTargetName() {
          return Messages.DebugTargetImpl_TargetName + " # " + backendId;
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

              @Override
              public ScriptNameManipulator getScriptNameManipulator() {
                return STANDALONE_SCRIPT_NAME_MANIPULATOR;
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

  private static final ScriptNameManipulator STANDALONE_SCRIPT_NAME_MANIPULATOR =
      new ScriptNameManipulator() {
    @Override
    public FilePath getFileName(String scriptName) {
      return new StringBasedFileName(scriptName);
    }

    @Override
    public ScriptNamePattern createPattern(List<String> components) {
      String pathString = join(components, "/");
      return new ScriptNamePattern(JavaScriptRegExpSupport.encodeLiteral(pathString) + "/?$");
    }
  };

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

  private static class StringBasedFileName implements ScriptNameManipulator.FilePath {
    private final String fullName;
    private final int lastPos;

    public StringBasedFileName(String fullName) {
      this.fullName = fullName;
      this.lastPos = fullName.lastIndexOf('/');
    }

    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        private int currentPos = lastPos;
        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }

        @Override
        public String next() {
          int nextPos = fullName.lastIndexOf('/', currentPos - 1);
          String result;
          if (nextPos == -1) {
            result = fullName.substring(0, currentPos);
          } else {
            result = fullName.substring(nextPos + 1, currentPos);
          }
          currentPos = nextPos;
          return result;
        }

        @Override
        public boolean hasNext() {
          return currentPos != -1;
        }
      };
    }

    @Override
    public String getLastComponent() {
      if (lastPos == -1) {
        return fullName;
      } else {
        return fullName.substring(lastPos + 1);
      }
    }
  }
}
