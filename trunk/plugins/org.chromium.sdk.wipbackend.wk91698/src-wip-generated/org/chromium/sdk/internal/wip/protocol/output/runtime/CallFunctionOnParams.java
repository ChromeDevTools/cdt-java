// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/91698/trunk/Source/WebCore/inspector/Inspector.json@91673

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Call function with given declaration on the given object.
 */
public class CallFunctionOnParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.runtime.CallFunctionOnData> {
  /**
   @param objectId Identifier of the object to call function on.
   @param functionDeclaration Declaration of the function to call.
   @param argumentsOpt Call arguments. All call arguments must belong to the same JavaScript world as the target object.
   */
  public CallFunctionOnParams(String objectId, String functionDeclaration, java.util.List<String> argumentsOpt) {
    this.put("objectId", objectId);
    this.put("functionDeclaration", functionDeclaration);
    if (argumentsOpt != null) {
      this.put("arguments", argumentsOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.RUNTIME + ".callFunctionOn";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.runtime.CallFunctionOnData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseRuntimeCallFunctionOnData(data.getUnderlyingObject());
  }

}
