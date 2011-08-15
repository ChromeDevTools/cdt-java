// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/87771/trunk/Source/WebCore/inspector/Inspector.json@86959

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Returns content of the given resource.
 */
public class GetResourceContentParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.page.GetResourceContentData> {
  /**
   @param frameId Frame id to get resource for.
   @param url URL of the resource to get content for.
   @param base64EncodeOpt Requests that resource content is served as base64.
   */
  public GetResourceContentParams(String frameId, String url, Boolean base64EncodeOpt) {
    this.put("frameId", frameId);
    this.put("url", url);
    if (base64EncodeOpt != null) {
      this.put("base64Encode", base64EncodeOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".getResourceContent";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.page.GetResourceContentData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parsePageGetResourceContentData(data.getUnderlyingObject());
  }

}
