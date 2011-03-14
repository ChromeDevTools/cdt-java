package org.chromium.sdk.internal.wip.protocol.input;

import org.chromium.sdk.internal.wip.protocol.WipProtocol;
import org.junit.Test;

public class WipProtocolTest {
  @Test
  public void buildParser() {
    // Just make sure parser is built.
    WipProtocol.PARSER.hashCode();
  }
}
