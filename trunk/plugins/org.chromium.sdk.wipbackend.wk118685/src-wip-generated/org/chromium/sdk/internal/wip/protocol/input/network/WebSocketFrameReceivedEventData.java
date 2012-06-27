// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Fired when WebSocket frame is received.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface WebSocketFrameReceivedEventData {
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

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameReceivedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameReceivedEventData>("Network.webSocketFrameReceived", org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameReceivedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.network.WebSocketFrameReceivedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseNetworkWebSocketFrameReceivedEventData(obj);
    }
  };
}
