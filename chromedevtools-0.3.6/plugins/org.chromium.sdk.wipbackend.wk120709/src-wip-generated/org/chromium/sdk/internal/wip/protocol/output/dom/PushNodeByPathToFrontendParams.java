// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Requests that the node is sent to the caller given its path. // FIXME, use XPath
 */
public class PushNodeByPathToFrontendParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.dom.PushNodeByPathToFrontendData> {
  /**
   @param path Path to node in the proprietary format.
   */
  public PushNodeByPathToFrontendParams(String path) {
    this.put("path", path);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".pushNodeByPathToFrontend";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.dom.PushNodeByPathToFrontendData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDOMPushNodeByPathToFrontendData(data.getUnderlyingObject());
  }

}
