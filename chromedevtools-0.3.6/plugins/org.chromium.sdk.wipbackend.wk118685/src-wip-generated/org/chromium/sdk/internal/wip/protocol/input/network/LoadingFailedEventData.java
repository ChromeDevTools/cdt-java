// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Fired when HTTP request has failed to load.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface LoadingFailedEventData {
  /**
   Request identifier.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.RequestIdTypedef*/ requestId();

  /**
   Timestamp.
   */
  Number/*See org.chromium.sdk.internal.wip.protocol.common.network.TimestampTypedef*/ timestamp();

  /**
   User friendly error message.
   */
  String errorText();

  /**
   True if loading was canceled.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Boolean canceled();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.LoadingFailedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.LoadingFailedEventData>("Network.loadingFailed", org.chromium.sdk.internal.wip.protocol.input.network.LoadingFailedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.network.LoadingFailedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseNetworkLoadingFailedEventData(obj);
    }
  };
}
