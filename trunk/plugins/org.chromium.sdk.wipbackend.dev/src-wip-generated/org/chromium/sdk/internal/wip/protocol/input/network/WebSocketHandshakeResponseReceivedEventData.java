// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Fired when WebSocket handshake response becomes available.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface WebSocketHandshakeResponseReceivedEventData {
  /**
   Request identifier.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.RequestIdTypedef*/ requestId();

  /**
   Timestamp.
   */
  Number/*See org.chromium.sdk.internal.wip.protocol.common.network.TimestampTypedef*/ timestamp();

  /**
   WebSocket response data.
   */
  org.chromium.sdk.internal.wip.protocol.input.network.WebSocketResponseValue response();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketHandshakeResponseReceivedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketHandshakeResponseReceivedEventData>("Network.webSocketHandshakeResponseReceived", org.chromium.sdk.internal.wip.protocol.input.network.WebSocketHandshakeResponseReceivedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.network.WebSocketHandshakeResponseReceivedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseNetworkWebSocketHandshakeResponseReceivedEventData(obj);
    }
  };
}
