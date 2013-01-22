// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@140428

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Requests that children of the node with given id are returned to the caller in form of <code>setChildNodes</code> events where not only immediate children are retrieved, but all children down to the specified depth.
 */
public class RequestChildNodesParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeId Id of the node to get children for.
   @param depthOpt The maximum depth at which children should be retrieved, defaults to 1. Use -1 for the entire subtree or provide an integer larger than 0.
   */
  public RequestChildNodesParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId, Long depthOpt) {
    this.put("nodeId", nodeId);
    if (depthOpt != null) {
      this.put("depth", depthOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".requestChildNodes";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
