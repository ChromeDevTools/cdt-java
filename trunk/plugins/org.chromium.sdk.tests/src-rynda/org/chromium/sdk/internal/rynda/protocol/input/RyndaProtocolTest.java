package org.chromium.sdk.internal.rynda.protocol.input;

import org.chromium.sdk.internal.rynda.protocol.RyndaProtocol;
import org.junit.Test;

public class RyndaProtocolTest {
  @Test
  public void buildParser() {
    // Just make sure parser is built.
    RyndaProtocol.PARSER.hashCode();
  }
}
