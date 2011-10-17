// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Pushes search results initiated using <code>performSearch</code> to the client.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface SearchResultsEventData {
  /**
   Ids of the search result nodes.
   */
  java.util.List<Long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/> nodeIds();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.SearchResultsEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.SearchResultsEventData>("DOM.searchResults", org.chromium.sdk.internal.wip.protocol.input.dom.SearchResultsEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.SearchResultsEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMSearchResultsEventData(obj);
    }
  };
}
