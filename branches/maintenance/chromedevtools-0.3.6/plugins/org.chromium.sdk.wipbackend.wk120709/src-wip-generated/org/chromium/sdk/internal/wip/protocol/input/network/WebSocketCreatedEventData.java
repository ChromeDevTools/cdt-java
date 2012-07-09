// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Fired upon WebSocket creation.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface WebSocketCreatedEventData {
  /**
   Request identifier.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.RequestIdTypedef*/ requestId();

  /**
   WebSocket request URL.
   */
  String url();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketCreatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.WebSocketCreatedEventData>("Network.webSocketCreated", org.chromium.sdk.internal.wip.protocol.input.network.WebSocketCreatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.network.WebSocketCreatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseNetworkWebSocketCreatedEventData(obj);
    }
  };
}
