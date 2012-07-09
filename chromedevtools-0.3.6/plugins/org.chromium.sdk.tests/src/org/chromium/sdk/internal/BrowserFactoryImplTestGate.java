package org.chromium.sdk.internal;

import org.chromium.sdk.Browser;
import org.chromium.sdk.internal.shellprotocol.ConnectionFactory;

public class BrowserFactoryImplTestGate {
  public static Browser create(BrowserFactoryImpl browserFactoryImpl, ConnectionFactory connectionFactory) {
    return browserFactoryImpl.create(connectionFactory);
  }
}
