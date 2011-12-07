// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Resolves JavaScript node object for given node id.
 */
public class ResolveNodeParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.dom.ResolveNodeData> {
  /**
   @param nodeId Id of the node to resolve.
   @param objectGroupOpt Symbolic group name that can be used to release multiple objects.
   */
  public ResolveNodeParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId, String objectGroupOpt) {
    this.put("nodeId", nodeId);
    if (objectGroupOpt != null) {
      this.put("objectGroup", objectGroupOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".resolveNode";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.dom.ResolveNodeData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDOMResolveNodeData(data.getUnderlyingObject());
  }

}
