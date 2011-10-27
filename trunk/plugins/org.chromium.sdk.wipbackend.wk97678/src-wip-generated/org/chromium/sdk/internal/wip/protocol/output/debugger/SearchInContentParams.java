// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Searches for given string in script content.
 */
public class SearchInContentParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.SearchInContentData> {
  /**
   @param scriptId Id of the script to search in.
   @param query String to search for.
   */
  public SearchInContentParams(String/*See org.chromium.sdk.internal.wip.protocol.output.debugger.ScriptIdTypedef*/ scriptId, String query) {
    this.put("scriptId", scriptId);
    this.put("query", query);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".searchInContent";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.SearchInContentData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerSearchInContentData(data.getUnderlyingObject());
  }

}
