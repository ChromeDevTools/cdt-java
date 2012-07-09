// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Moves node into the new container, places it before the given anchor.
 */
public class MoveToParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.dom.MoveToData> {
  /**
   @param nodeId Id of the node to drop.
   @param targetNodeId Id of the element to drop into.
   @param insertBeforeNodeIdOpt Drop node before given one.
   */
  public MoveToParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId, long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ targetNodeId, Long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ insertBeforeNodeIdOpt) {
    this.put("nodeId", nodeId);
    this.put("targetNodeId", targetNodeId);
    if (insertBeforeNodeIdOpt != null) {
      this.put("insertBeforeNodeId", insertBeforeNodeIdOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".moveTo";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.dom.MoveToData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDOMMoveToData(data.getUnderlyingObject());
  }

}
