// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Edits JavaScript script live.
 */
public class EditScriptSourceParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.EditScriptSourceData> {
  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".editScriptSource";

  /**
   @param sourceID Id of the script to edit.
   @param scriptSource New content of the script.
   */
  public EditScriptSourceParams(String sourceID, String scriptSource) {
    this.put("sourceID", sourceID);
    this.put("scriptSource", scriptSource);
  }

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.EditScriptSourceData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parse(data.getUnderlyingObject(), org.chromium.sdk.internal.wip.protocol.input.debugger.EditScriptSourceData.class);
  }

}
