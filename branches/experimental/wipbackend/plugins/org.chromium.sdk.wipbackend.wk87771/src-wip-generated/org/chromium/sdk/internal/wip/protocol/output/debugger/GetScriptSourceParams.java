// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/87771/trunk/Source/WebCore/inspector/Inspector.json@86959

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Returns source for the script with given ID.
 */
public class GetScriptSourceParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.GetScriptSourceData> {
  /**
   @param sourceId Id of the script to get source for.
   */
  public GetScriptSourceParams(String sourceId) {
    this.put("sourceId", sourceId);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".getScriptSource";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.GetScriptSourceData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerGetScriptSourceData(data.getUnderlyingObject());
  }

}
