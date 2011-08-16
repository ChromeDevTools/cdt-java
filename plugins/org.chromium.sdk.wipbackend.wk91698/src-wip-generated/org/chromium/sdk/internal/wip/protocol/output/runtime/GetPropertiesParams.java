// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@93101

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Returns properties of a given object.
 */
public class GetPropertiesParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.runtime.GetPropertiesData> {
  /**
   @param objectId Identifier of the object to return properties for.
   @param ownPropertiesOpt If true, returns properties belonging only to the element itself, not to its prototype chain.
   */
  public GetPropertiesParams(String/*See org.chromium.sdk.internal.wip.protocol.output.runtime.RemoteObjectIdTypedef*/ objectId, Boolean ownPropertiesOpt) {
    this.put("objectId", objectId);
    if (ownPropertiesOpt != null) {
      this.put("ownProperties", ownPropertiesOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.RUNTIME + ".getProperties";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.runtime.GetPropertiesData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseRuntimeGetPropertiesData(data.getUnderlyingObject());
  }

}
