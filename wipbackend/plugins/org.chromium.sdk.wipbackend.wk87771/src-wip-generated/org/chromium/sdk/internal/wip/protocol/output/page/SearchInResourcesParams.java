// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/92284/trunk/Source/WebCore/inspector/Inspector.json@92284

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Searches for given string in frame / resource tree structure.
 */
public class SearchInResourcesParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.page.SearchInResourcesData> {
  /**
   @param text String to search for.
   @param caseSensitiveOpt If true, search is case sensitive.
   @param isRegexOpt If true, treats string parameter as regex.
   */
  public SearchInResourcesParams(String text, Boolean caseSensitiveOpt, Boolean isRegexOpt) {
    this.put("text", text);
    if (caseSensitiveOpt != null) {
      this.put("caseSensitive", caseSensitiveOpt);
    }
    if (isRegexOpt != null) {
      this.put("isRegex", isRegexOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".searchInResources";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.page.SearchInResourcesData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parsePageSearchInResourcesData(data.getUnderlyingObject());
  }

}
