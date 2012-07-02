// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Returns event listeners relevant to the node.
 */
public class GetEventListenersForNodeParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.dom.GetEventListenersForNodeData> {
  /**
   @param nodeId Id of the node to get listeners for.
   */
  public GetEventListenersForNodeParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId) {
    this.put("nodeId", nodeId);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".getEventListenersForNode";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.dom.GetEventListenersForNodeData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDOMGetEventListenersForNodeData(data.getUnderlyingObject());
  }

}
