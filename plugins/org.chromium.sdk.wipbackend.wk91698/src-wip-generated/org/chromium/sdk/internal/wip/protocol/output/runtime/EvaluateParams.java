// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/91698/trunk/Source/WebCore/inspector/Inspector.json@91673

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Evaluate expression on global object.
 */
public class EvaluateParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData> {
  /**
   @param expression Expression to evaluate.
   @param objectGroupOpt Symbolic group name that can be used to release multiple objects.
   @param includeCommandLineAPIOpt Determines whether Command Line API should be available during the evaluation.
   @param doNotPauseOnExceptionsOpt Specifies whether evaluation should stop on exceptions. Overrides setPauseOnException state.
   */
  public EvaluateParams(String expression, String objectGroupOpt, Boolean includeCommandLineAPIOpt, Boolean doNotPauseOnExceptionsOpt) {
    this.put("expression", expression);
    if (objectGroupOpt != null) {
      this.put("objectGroup", objectGroupOpt);
    }
    if (includeCommandLineAPIOpt != null) {
      this.put("includeCommandLineAPI", includeCommandLineAPIOpt);
    }
    if (doNotPauseOnExceptionsOpt != null) {
      this.put("doNotPauseOnExceptions", doNotPauseOnExceptionsOpt);
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
