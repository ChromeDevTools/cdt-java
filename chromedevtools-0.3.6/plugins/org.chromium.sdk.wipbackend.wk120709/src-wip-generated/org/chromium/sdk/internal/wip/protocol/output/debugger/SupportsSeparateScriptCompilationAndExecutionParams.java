// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@121014

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Tells whether debugger supports separate script compilation and execution.
 */
public class SupportsSeparateScriptCompilationAndExecutionParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.SupportsSeparateScriptCompilationAndExecutionData> {
  public SupportsSeparateScriptCompilationAndExecutionParams() {
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".supportsSeparateScriptCompilationAndExecution";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.SupportsSeparateScriptCompilationAndExecutionData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerSupportsSeparateScriptCompilationAndExecutionData(data.getUnderlyingObject());
  }

}
