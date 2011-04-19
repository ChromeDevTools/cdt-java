// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.output.page;

public class GetCookiesParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.page.GetCookiesData> {
  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".getCookies";

  public GetCookiesParams() {
  }

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.page.GetCookiesData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parse(data.getUnderlyingObject(), org.chromium.sdk.internal.wip.protocol.input.page.GetCookiesData.class);
  }

}
