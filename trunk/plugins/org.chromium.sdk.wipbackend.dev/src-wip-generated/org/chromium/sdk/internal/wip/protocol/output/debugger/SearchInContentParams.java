// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Searches for given string in script content.
 */
public class SearchInContentParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.SearchInContentData> {
  /**
   @param scriptId Id of the script to search in.
   @param query String to search for.
   @param caseSensitiveOpt If true, search is case sensitive.
   @param isRegexOpt If true, treats string parameter as regex.
   */
  public SearchInContentParams(String/*See org.chromium.sdk.internal.wip.protocol.common.debugger.ScriptIdTypedef*/ scriptId, String query, Boolean caseSensitiveOpt, Boolean isRegexOpt) {
    this.put("scriptId", scriptId);
    this.put("query", query);
    if (caseSensitiveOpt != null) {
      this.put("caseSensitive", caseSensitiveOpt);
    }
    if (isRegexOpt != null) {
      this.put("isRegex", isRegexOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".searchInContent";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.SearchInContentData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerSearchInContentData(data.getUnderlyingObject());
  }

}
