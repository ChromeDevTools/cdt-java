// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/92284/trunk/Source/WebCore/inspector/Inspector.json@92284

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Returns all browser cookies. Depending on the backend support, will either return detailed cookie information in the <code>cookie</code> field or string cookie representation using <code>cookieString</code>.
 */
public class GetCookiesParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.page.GetCookiesData> {
  public GetCookiesParams() {
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".getCookies";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.page.GetCookiesData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parsePageGetCookiesData(data.getUnderlyingObject());
  }

}
