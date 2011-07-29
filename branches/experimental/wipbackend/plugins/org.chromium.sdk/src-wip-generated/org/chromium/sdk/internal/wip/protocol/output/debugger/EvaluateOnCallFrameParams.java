// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84481

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Evaluates expression on a given call frame.
 */
public class EvaluateOnCallFrameParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.EvaluateOnCallFrameData> {
  /**
   @param callFrameId Call frame identifier to evaluate on. This identifier is a part of backtrace reported by the <code>pausedScript</code>.
   @param expression Expression to evaluate.
   @param objectGroupOpt String object group name to put result into (allows rapid releasing resulting object handles using <code>releaseObjectGroup</code>).
   @param includeCommandLineAPIOpt Specifies whether command line API should be available to the evaluated expression, defaults to false.
   */
  public EvaluateOnCallFrameParams(String callFrameId, String expression, String objectGroupOpt, Boolean includeCommandLineAPIOpt) {
    this.put("callFrameId", callFrameId);
    this.put("expression", expression);
    if (objectGroupOpt != null) {
      this.put("objectGroup", objectGroupOpt);
    }
    if (includeCommandLineAPIOpt != null) {
      this.put("includeCommandLineAPI", includeCommandLineAPIOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".evaluateOnCallFrame";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.EvaluateOnCallFrameData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parse(data.getUnderlyingObject(), org.chromium.sdk.internal.wip.protocol.input.debugger.EvaluateOnCallFrameData.class);
  }

}
