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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;


public class JavascriptVmEmbedderFactory {

  public static final String LOCALHOST = "127.0.0.1"; //$NON-NLS-1$

  public static JavascriptVmEmbedder.Attachable connectToChromeDevTools(int port,
      ConnectionLogger logger, final TabSelector tabSelector) throws CoreException {

    SocketAddress address = new InetSocketAddress(LOCALHOST, port);
    final Browser browser = browserCache.getOrCreateBrowser(address, logger);
    try {
      browser.connect();
    } catch (UnsupportedVersionException e) {
      throw newCoreException(e);
    } catch (IOException e) {
      throw newCoreException(e);
    }

    return new JavascriptVmEmbedder.Attachable() {

      public JavascriptVmEmbedder selectVm() throws CoreException {
        BrowserTab[] tabs = getBrowserTabs(browser);
        final BrowserTab targetTab = tabSelector.selectTab(tabs);
        if (targetTab == null) {
          return null;
        }

        return new EmbeddingTab(targetTab);
      }

      private BrowserTab[] getBrowserTabs(Browser browser) throws CoreException {
        BrowserTab[] tabs;
        try {
          tabs = browser.getTabs();
        } catch (IOException e) {
          throw newCoreException("Failed to get tabs for debugging", e); //$NON-NLS-1$
        } catch (IllegalStateException e) {
          throw newCoreException(
              "Another Chromium JavaScript Debug Launch is in progress", e); //$NON-NLS-1$
        }
        return tabs;
      }
    };
  }

  private static final class EmbeddingTab implements JavascriptVmEmbedder {
    private final BrowserTab targetTab;

    EmbeddingTab(BrowserTab targetTab) {
      this.targetTab = targetTab;
    }

    public boolean attach(final Listener embedderListener,
        final DebugEventListener debugEventListener) {
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
      return targetTab.attach(tabDebugEventListener);
    }

    public JavascriptVm getJavascriptVm() {
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
      ConnectionLogger connectionLogger) {
    SocketAddress address = new InetSocketAddress(LOCALHOST, port);
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

            return MessageFormat.format(Messages.JavascriptVmEmbedderFactory_TargetName0,
                standaloneVm.getEmbedderName(), standaloneVm.getVmVersion());
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
    synchronized Browser getOrCreateBrowser(SocketAddress address,
        ConnectionLogger connectionLogger) throws CoreException {
      if (connectionLogger == null) {
        Browser result = address2Browser.get(address);
        if (result == null) {
          result = createBrowserImpl(address, connectionLogger);
          address2Browser.put(address, result);
        }
        return result;
      } else {
        Browser oldBrowser = address2BrowserWithLogger.get(address);
        if (oldBrowser != null) {
          // TODO(peter.rybin): get the actual value here.
          boolean isBrowserConnected = false;
          if (isBrowserConnected) {
            throw newCoreException(
                "Can't create second debug target to the same browser" //$NON-NLS-1$
                + " when network console is on", //$NON-NLS-1$
                null);
          }
        }
        Browser result = createBrowserImpl(address, connectionLogger);
        address2BrowserWithLogger.put(address, result);
        return result;
      }
    }
    private Browser createBrowserImpl(SocketAddress address, ConnectionLogger connectionLogger) {
      return BrowserFactory.getInstance().create(address, connectionLogger);
    }

    private final Map<SocketAddress, Browser> address2Browser =
        new HashMap<SocketAddress, Browser>();

    private final Map<SocketAddress, Browser> address2BrowserWithLogger =
        new HashMap<SocketAddress, Browser>();
  }
}
