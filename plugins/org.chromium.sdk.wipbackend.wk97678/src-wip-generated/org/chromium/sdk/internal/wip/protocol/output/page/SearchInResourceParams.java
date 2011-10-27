// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Searches for given string in resource content.
 */
public class SearchInResourceParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.page.SearchInResourceData> {
  /**
   @param frameId Frame id for resource to search in.
   @param url URL of the resource to search in.
   @param query String to search for.
   */
  public SearchInResourceParams(String frameId, String url, String query) {
    this.put("frameId", frameId);
    this.put("url", url);
    this.put("query", query);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".searchInResource";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.page.SearchInResourceData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parsePageSearchInResourceData(data.getUnderlyingObject());
  }

}
