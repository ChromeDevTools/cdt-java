// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Fired when WebSocket frame is sent.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface WebSocketFrameSentEventData {
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
  org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameValue response();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameSentEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameSentEventData>("Network.webSocketFrameSent", org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameSentEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameSentEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseNetworkWebSocketFrameSentEventData(obj);
    }
  };
}
