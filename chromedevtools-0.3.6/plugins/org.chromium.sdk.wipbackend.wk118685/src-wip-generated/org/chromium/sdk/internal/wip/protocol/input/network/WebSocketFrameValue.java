// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 WebSocket frame data.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface WebSocketFrameValue {
  /**
   WebSocket frame opcode.
   */
  Number opcode();

  /**
   WebSocke frame mask.
   */
  boolean mask();

  /**
   WebSocke frame payload data.
   */
  String payloadData();

}
