// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@136521

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Highlights DOM node with given id or with the given JavaScript object wrapper. Either nodeId or objectId must be specified.
 */
public class HighlightNodeParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeIdOpt Identifier of the node to highlight.
   @param objectIdOpt JavaScript object id of the node to be highlighted.
   @param highlightConfig A descriptor for the highlight appearance.
   */
  public HighlightNodeParams(Long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeIdOpt, String/*See org.chromium.sdk.internal.wip.protocol.common.runtime.RemoteObjectIdTypedef*/ objectIdOpt, org.chromium.sdk.internal.wip.protocol.output.dom.HighlightConfigParam highlightConfig) {
    if (nodeIdOpt != null) {
      this.put("nodeId", nodeIdOpt);
    }
    if (objectIdOpt != null) {
      this.put("objectId", objectIdOpt);
    }
    this.put("highlightConfig", highlightConfig);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".highlightNode";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
