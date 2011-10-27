// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 WebSocket request data.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface WebSocketRequestValue {
  /**
   HTTP response status text.
   */
  String requestKey3();

  /**
   HTTP response headers.
   */
  org.chromium.sdk.internal.wip.protocol.input.network.HeadersValue headers();

}
