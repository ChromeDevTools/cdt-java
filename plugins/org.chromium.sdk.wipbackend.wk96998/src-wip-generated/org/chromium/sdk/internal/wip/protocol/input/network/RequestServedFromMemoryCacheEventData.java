// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Fired when HTTP request has been served from memory cache.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface RequestServedFromMemoryCacheEventData {
  /**
   Request identifier.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.input.network.RequestIdTypedef*/ requestId();

  /**
   Frame identifier.
   */
  String frameId();

  /**
   Loader identifier.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.input.network.LoaderIdTypedef*/ loaderId();

  /**
   URL of the document this request is loaded for.
   */
  String documentURL();

  /**
   Timestamp.
   */
  Number/*See org.chromium.sdk.internal.wip.protocol.input.network.TimestampTypedef*/ timestamp();

  /**
   Request initiator.
   */
  org.chromium.sdk.internal.wip.protocol.input.network.InitiatorValue initiator();

  /**
   Cached resource data.
   */
  org.chromium.sdk.internal.wip.protocol.input.network.CachedResourceValue resource();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.RequestServedFromMemoryCacheEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.network.RequestServedFromMemoryCacheEventData>("Network.requestServedFromMemoryCache", org.chromium.sdk.internal.wip.protocol.input.network.RequestServedFromMemoryCacheEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.network.RequestServedFromMemoryCacheEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseNetworkRequestServedFromMemoryCacheEventData(obj);
    }
  };
}
