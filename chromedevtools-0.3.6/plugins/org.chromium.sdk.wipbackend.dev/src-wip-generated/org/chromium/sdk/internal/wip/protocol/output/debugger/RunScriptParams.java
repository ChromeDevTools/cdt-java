// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@121014

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Runs script with given id in a given context.
 */
public class RunScriptParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.RunScriptData> {
  /**
   @param contextIdOpt Specifies in which isolated context to perform script run. Each content script lives in an isolated context and this parameter may be used to specify on of those contexts. If the parameter is omitted or 0 the evaluation will be performed in the context of the inspected page.
   @param scriptId Id of the script to run.
   @param objectGroupOpt Symbolic group name that can be used to release multiple objects.
   @param doNotPauseOnExceptionsAndMuteConsoleOpt Specifies whether script run should stop on exceptions and mute console. Overrides setPauseOnException state.
   */
  public RunScriptParams(Long/*See org.chromium.sdk.internal.wip.protocol.common.runtime.ExecutionContextIdTypedef*/ contextIdOpt, String/*See org.chromium.sdk.internal.wip.protocol.common.debugger.ScriptIdTypedef*/ scriptId, String objectGroupOpt, Boolean doNotPauseOnExceptionsAndMuteConsoleOpt) {
    if (contextIdOpt != null) {
      this.put("contextId", contextIdOpt);
    }
    this.put("scriptId", scriptId);
    if (objectGroupOpt != null) {
      this.put("objectGroup", objectGroupOpt);
    }
    if (doNotPauseOnExceptionsAndMuteConsoleOpt != null) {
      this.put("doNotPauseOnExceptionsAndMuteConsole", doNotPauseOnExceptionsAndMuteConsoleOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".runScript";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.RunScriptData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerRunScriptData(data.getUnderlyingObject());
  }

}
