package org.chromium.sdk.internal;

import org.chromium.sdk.internal.standalonev8.StandaloneVmImpl;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Handshaker;

public class BrowserFactoryImplTestGate {
  public static StandaloneVmImpl createStandalone(Connection connection,
      Handshaker.StandaloneV8 handshaker) {
    return BrowserFactoryImpl.INSTANCE.createStandalone(connection, handshaker);
  }
}
