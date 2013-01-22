// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@140428

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Highlights DOM node with given id or with the given JavaScript object wrapper. Either nodeId or objectId must be specified.
 */
public class HighlightNodeParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param highlightConfig A descriptor for the highlight appearance.
   @param nodeIdOpt Identifier of the node to highlight.
   @param objectIdOpt JavaScript object id of the node to be highlighted.
   */
  public HighlightNodeParams(org.chromium.sdk.internal.wip.protocol.output.dom.HighlightConfigParam highlightConfig, Long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeIdOpt, String/*See org.chromium.sdk.internal.wip.protocol.common.runtime.RemoteObjectIdTypedef*/ objectIdOpt) {
    this.put("highlightConfig", highlightConfig);
    if (nodeIdOpt != null) {
      this.put("nodeId", nodeIdOpt);
    }
    if (objectIdOpt != null) {
      this.put("objectId", objectIdOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".highlightNode";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
