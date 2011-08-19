package org.chromium.debug.jsdtbridge;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.chromium.debug.jsdtbridge.messages"; //$NON-NLS-1$
  public static String JsdtFormatterBridge_FALLBACK_COMMENT;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
