package org.chromium.debug.core.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.JavascriptVmEmbedder.Attachable;
import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.StandaloneVm;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.Browser.TabFetcher;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;


public class JavascriptVmEmbedderFactory {

  private static final String LOCALHOST = "127.0.0.1"; //$NON-NLS-1$

  public static JavascriptVmEmbedder.Attachable connectToChromeDevTools(int port,
      ConnectionLogger logger, final TabSelector tabSelector,
      NamedConnectionLoggerFactory connectionLoggerFactory) throws CoreException {

    SocketAddress address = new InetSocketAddress(LOCALHOST, port);
    final Browser browser = browserCache.getOrCreateBrowser(address, connectionLoggerFactory);
    final TabFetcher tabFetcher;
    try {
      tabFetcher = browser.createTabFetcher();
    } catch (UnsupportedVersionException e) {
      throw newCoreException(e);
    } catch (IOException e) {
      throw newCoreException(e);
    }

    return new JavascriptVmEmbedder.Attachable() {

      public JavascriptVmEmbedder selectVm() throws CoreException {
        // TODO(peter.rybin): call TabFetcher#dismiss here to release connection properly.
        Browser.TabConnector targetTabConnector;
        try {
          targetTabConnector = tabSelector.selectTab(tabFetcher);
        } catch (IOException e) {
          throw newCoreException("Failed to get tabs for debugging", e); //$NON-NLS-1$
        }
        if (targetTabConnector == null) {
          return null;
        }

        return new EmbeddingTab(targetTabConnector);
      }
    };
  }

  private static final class EmbeddingTab implements JavascriptVmEmbedder {
    private final Browser.TabConnector targetTabConnector;
    private BrowserTab targetTab = null;

    EmbeddingTab(Browser.TabConnector targetTabConnector) {
      this.targetTabConnector = targetTabConnector;
    }

    public boolean attach(final Listener embedderListener,
        final DebugEventListener debugEventListener) throws IOException {
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
      targetTab = targetTabConnector.attach(tabDebugEventListener);
      return targetTab != null;
    }

    public JavascriptVm getJavascriptVm() {
      if (targetTab == null) {
        throw new IllegalStateException("Tab not attached yet"); //$NON-NLS-1$
      }
      return targetTab;
    }

    public String getTargetName() {
      return Messages.DebugTargetImpl_TargetName;
    }

    public String getThreadName() {
      return targetTab.getUrl();
    }
  }

  public static Attachable connectToStandalone(int port,
      NamedConnectionLoggerFactory connectionLoggerFactory) {
    SocketAddress address = new InetSocketAddress(LOCALHOST, port);
    ConnectionLogger connectionLogger =
      connectionLoggerFactory.createLogger(address.toString());
    final StandaloneVm standaloneVm = BrowserFactory.getInstance().createStandalone(address,
        connectionLogger);

    return new Attachable() {
      public JavascriptVmEmbedder selectVm() {
        return new JavascriptVmEmbedder() {
          public boolean attach(Listener embedderListener, DebugEventListener debugEventListener) {
            embedderListener = null;
            return standaloneVm.attach(debugEventListener);
          }
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
              targetTitle = MessageFormat.format(Messages.JavascriptVmEmbedderFactory_TargetName0,
                  embedderName, vmVersion);
            }
            boolean isAttached = standaloneVm.isAttached();
            if (!isAttached) {
              String disconnectMessage;
              if (disconnectReason == null) {
                disconnectMessage = Messages.JavascriptVmEmbedderFactory_Terminated;
              } else {
                disconnectMessage = MessageFormat.format(
                    Messages.JavascriptVmEmbedderFactory_TerminatedWithReason, disconnectReason);
              }
              targetTitle = "<" + disconnectMessage + "> " + targetTitle; //$NON-NLS-1$//$NON-NLS-2$
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

  private static CoreException newCoreException(String message, Throwable cause) {
    return new CoreException(
        new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID, message, cause));
  }
  private static CoreException newCoreException(Exception e) {
    return new CoreException(
        new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
            "Failed to connect to the remote browser", e)); //$NON-NLS-1$
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