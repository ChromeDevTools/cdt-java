// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Tells whether <code>setScriptSource</code> is supported.
 */
public class CanSetScriptSourceParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.CanSetScriptSourceData> {
  public CanSetScriptSourceParams() {
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".canSetScriptSource";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.CanSetScriptSourceData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerCanSetScriptSourceData(data.getUnderlyingObject());
  }

}
