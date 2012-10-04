// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Removes node with given id.
 */
public class RemoveNodeParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeId Id of the node to remove.
   */
  public RemoveNodeParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId) {
    this.put("nodeId", nodeId);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".removeNode";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
