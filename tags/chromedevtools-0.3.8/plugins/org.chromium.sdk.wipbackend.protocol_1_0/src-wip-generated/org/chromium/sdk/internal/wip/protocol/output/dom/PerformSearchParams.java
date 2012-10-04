// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Searches for a given string in the DOM tree. Use <code>getSearchResults</code> to access search results or <code>cancelSearch</code> to end this search session.
 */
public class PerformSearchParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.dom.PerformSearchData> {
  /**
   @param query Plain text or query selector or XPath search query.
   */
  public PerformSearchParams(String query) {
    this.put("query", query);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".performSearch";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.dom.PerformSearchData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDOMPerformSearchData(data.getUnderlyingObject());
  }

}
