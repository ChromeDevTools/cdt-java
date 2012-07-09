// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Fired when data chunk was received over the network.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface DataReceivedEventData {
  /**
   Request identifier.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.RequestIdTypedef*/ requestId();

  /**
   Timestamp.
   */
  Number/*See org.chromium.sdk.internal.wip.protocol.common.network.TimestampTypedef*/ timestamp();

  /**
   Data chunk length.
   */
  long dataLength();

  /**
   Actual bytes received (might be less than dataLength for compressed encodings).
   */
  long encodedDataLength();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.DataReceivedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.DataReceivedEventData>("Network.dataReceived", org.chromium.sdk.internal.wip.protocol.input.network.DataReceivedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.network.DataReceivedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseNetworkDataReceivedEventData(obj);
    }
  };
}
