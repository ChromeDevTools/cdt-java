// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@92122

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Evaluates expression on global object.
 */
public class EvaluateParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData> {
  /**
   @param expression Expression to evaluate.
   @param objectGroupOpt Symbolic group name that can be used to release multiple objects.
   @param includeCommandLineAPIOpt Determines whether Command Line API should be available during the evaluation.
   @param doNotPauseOnExceptionsOpt Specifies whether evaluation should stop on exceptions. Overrides setPauseOnException state.
   @param frameIdOpt Specifies in which frame to perform evaluation.
   @param sendResultByValueOpt Whether the result is expected to be a JSON object which should be sent by value.
   */
  public EvaluateParams(String expression, String objectGroupOpt, Boolean includeCommandLineAPIOpt, Boolean doNotPauseOnExceptionsOpt, String frameIdOpt, Boolean sendResultByValueOpt) {
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
    if (frameIdOpt != null) {
      this.put("frameId", frameIdOpt);
    }
    if (sendResultByValueOpt != null) {
      this.put("sendResultByValue", sendResultByValueOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.RUNTIME + ".evaluate";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parse(data.getUnderlyingObject(), org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData.class);
  }

}
