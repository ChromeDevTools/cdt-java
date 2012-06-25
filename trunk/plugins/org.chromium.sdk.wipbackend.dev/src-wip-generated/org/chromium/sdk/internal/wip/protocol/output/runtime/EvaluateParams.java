// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@121014

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Evaluates expression on global object.
 */
public class EvaluateParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData> {
  /**
   @param expression Expression to evaluate.
   @param objectGroupOpt Symbolic group name that can be used to release multiple objects.
   @param includeCommandLineAPIOpt Determines whether Command Line API should be available during the evaluation.
   @param doNotPauseOnExceptionsAndMuteConsoleOpt Specifies whether evaluation should stop on exceptions and mute console. Overrides setPauseOnException state.
   @param contextIdOpt Specifies in which isolated context to perform evaluation. Each content script lives in an isolated context and this parameter may be used to specify on of those contexts. If the parameter is omitted or 0 the evaluation will be performed in the context of the inspected page.
   @param returnByValueOpt Whether the result is expected to be a JSON object that should be sent by value.
   */
  public EvaluateParams(String expression, String objectGroupOpt, Boolean includeCommandLineAPIOpt, Boolean doNotPauseOnExceptionsAndMuteConsoleOpt, Long/*See org.chromium.sdk.internal.wip.protocol.common.runtime.ExecutionContextIdTypedef*/ contextIdOpt, Boolean returnByValueOpt) {
    this.put("expression", expression);
    if (objectGroupOpt != null) {
      this.put("objectGroup", objectGroupOpt);
    }
    if (includeCommandLineAPIOpt != null) {
      this.put("includeCommandLineAPI", includeCommandLineAPIOpt);
    }
    if (doNotPauseOnExceptionsAndMuteConsoleOpt != null) {
      this.put("doNotPauseOnExceptionsAndMuteConsole", doNotPauseOnExceptionsAndMuteConsoleOpt);
    }
    if (contextIdOpt != null) {
      this.put("contextId", contextIdOpt);
    }
    if (returnByValueOpt != null) {
      this.put("returnByValue", returnByValueOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.RUNTIME + ".evaluate";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseRuntimeEvaluateData(data.getUnderlyingObject());
  }

}
