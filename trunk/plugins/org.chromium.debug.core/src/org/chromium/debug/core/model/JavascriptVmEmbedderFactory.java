package org.chromium.debug.core.model;

import java.io.IOException;
import java.text.MessageFormat;

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
  public static JavascriptVmEmbedder.Attachable connectToChromeDevTools(int port,
      ConnectionLogger logger, final TabSelector tabSelector) throws CoreException {

    final Browser browser = BrowserFactory.getInstance().create(port, logger);
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

  public static Attachable connectToStandalone(String host, int port,
      ConnectionLogger connectionLogger) {
    final StandaloneVm standaloneVm = BrowserFactory.getInstance().createStandalone(host, port,
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
}
