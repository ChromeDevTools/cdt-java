// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Returns search results from given <code>fromIndex</code> to given <code>toIndex</code> from the sarch with the given identifier.
 */
public class GetSearchResultsParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.dom.GetSearchResultsData> {
  /**
   @param searchId Unique search session identifier.
   @param fromIndex Start index of the search result to be returned.
   @param toIndex End index of the search result to be returned.
   */
  public GetSearchResultsParams(String searchId, long fromIndex, long toIndex) {
    this.put("searchId", searchId);
    this.put("fromIndex", fromIndex);
    this.put("toIndex", toIndex);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".getSearchResults";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.dom.GetSearchResultsData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDOMGetSearchResultsData(data.getUnderlyingObject());
  }

}
