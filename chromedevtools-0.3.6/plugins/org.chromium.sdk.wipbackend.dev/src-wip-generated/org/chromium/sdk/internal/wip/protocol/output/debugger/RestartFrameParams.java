// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@121014

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Restarts particular call frame from the beginning.
 */
public class RestartFrameParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.RestartFrameData> {
  /**
   @param callFrameId Call frame identifier to evaluate on.
   */
  public RestartFrameParams(String/*See org.chromium.sdk.internal.wip.protocol.common.debugger.CallFrameIdTypedef*/ callFrameId) {
    this.put("callFrameId", callFrameId);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".restartFrame";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.RestartFrameData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerRestartFrameData(data.getUnderlyingObject());
  }

}
