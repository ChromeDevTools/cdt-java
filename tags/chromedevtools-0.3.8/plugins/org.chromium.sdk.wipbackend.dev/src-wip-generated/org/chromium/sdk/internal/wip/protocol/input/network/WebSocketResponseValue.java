// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 WebSocket response data.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface WebSocketResponseValue {
  /**
   HTTP response status code.
   */
  Number status();

  /**
   HTTP response status text.
   */
  String statusText();

  /**
   HTTP response headers.
   */
  org.chromium.sdk.internal.wip.protocol.input.network.HeadersValue headers();

  /**
   Challenge response.
   */
  String challengeResponse();

}
